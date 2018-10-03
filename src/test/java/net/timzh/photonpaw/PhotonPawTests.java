package net.timzh.photonpaw;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class PhotonPawTests {
    private int port = 19081;

    @Test
    void test_bind_path() {
        String testStr = "+";
        String testDoc = "<html><body>" + testStr + "</body></html>";
        try (PhotonPaw paw = createBackend()) {
            paw.bindPath("/bind", "text/html", request -> testDoc).start();
            withHtmlPage("/bind", page ->
                assertEquals(testStr, page.asText())
            );
        }
    }

    @Test
    void test_handle_ui_command() {
        Trigger commandReceived = new Trigger();
        try (PhotonPaw paw = createBackend()) {
            paw.handleCommand("a", msg -> {
                assertEquals("ui command", msg);
                commandReceived.activate();
            }).start();

            withHtmlPage("/handle_ui_command.html", page ->
                commandReceived.assertActivated()
            );
        }
    }

    @Test
    void test_handle_server_command() {
        Trigger commandSent = new Trigger();
        Trigger commandReceived = new Trigger();
        String testMsg = "server command";
        try (PhotonPaw paw = createBackend()) {
            paw.handleCommand("b", msg -> {
                assertEquals(testMsg, msg);
                commandReceived.activate();
            }).start(() -> {
                paw.send("a", testMsg);
                commandSent.activate();
            });

            withHtmlPage("/handle_server_command.html", page -> {
                commandSent.assertActivated();
                commandReceived.assertActivated();
            });
        }
    }

    @Test
    void test_handle_ui_query() {
        Trigger queryReceived = new Trigger();
        Trigger queryResponseAccepted = new Trigger();
        String testMsg = "ui query";
        try (PhotonPaw paw = createBackend()) {
            paw.handleQuery("a", msg -> {
                assertEquals(testMsg, msg);
                queryReceived.activate();
                return msg;
            }).handleCommand("b", msg -> {
                assertEquals(testMsg, msg);
                queryResponseAccepted.activate();
            }).start();

            withHtmlPage("/handle_ui_query.html", page -> {
                queryReceived.assertActivated();
                queryResponseAccepted.assertActivated();
            });
        }
    }

    @Test
    void test_server_default_handler() {
        Trigger commandReceived = new Trigger();
        try (PhotonPaw paw = createBackend()) {
            paw.defaultHandler((event, msg) -> {
                assertEquals("a", event);
                assertEquals("ui command", msg);
                commandReceived.activate();
            }).start();

            withHtmlPage("/server_default_handler.html", page ->
                commandReceived.assertActivated()
            );
        }
    }

    @Test
    void test_ui_default_handler() {
        Trigger commandSent = new Trigger();
        Trigger commandReceived = new Trigger();
        String testEvent = "a";
        String testMsg = "server command";
        try (PhotonPaw paw = createBackend()) {
            paw.handleCommand("b", msg -> {
                assertEquals(testEvent + testMsg, msg);
                commandReceived.activate();
            }).start(() -> {
                paw.send(testEvent, testMsg);
                commandSent.activate();
            });

            withHtmlPage("/ui_default_handler.html", page -> {
                commandSent.assertActivated();
                commandReceived.assertActivated();
            });
        }
    }

    @Test
    void test_default_config() {
        Trigger commandReceived = new Trigger();
        String testScript = "<script>PhotonPaw.start().then(() => PhotonPaw.send('a', 'ui command'))</script>";
        String testDoc = "<html><body><script src='photonpaw_client.js'></script>" + testScript + "</body></html>";
        try (PhotonPaw paw = new PhotonPaw()) {
            paw.bindPath("/bind", "text/html", request -> testDoc).handleCommand("a", msg -> {
                assertEquals("ui command", msg);
                commandReceived.activate();
            }).start();

            withHtmlPage("/bind", paw.getPort(), page ->
                commandReceived.assertActivated()
            );
        }
    }

    @Test
    void test_multiple_instances() {
        Trigger command1Received = new Trigger();
        Trigger command2Received = new Trigger();
        try (PhotonPaw paw1 = new PhotonPaw().ports(port, port + 1).resourcesRoot("")) {
            try (PhotonPaw paw2 = new PhotonPaw().ports(port + 2, port + 3).resourcesRoot("")) {
                paw1.handleCommand("a", msg -> {
                    assertEquals("ui command", msg);
                    command1Received.activate();
                }).start();
                paw2.handleCommand("a", msg -> {
                    assertEquals("ui command", msg);
                    command2Received.activate();
                }).start();

                withHtmlPage("/handle_ui_command.html", port, page ->
                    command1Received.assertActivated()
                );
                withHtmlPage("/handle_ui_command.html", port + 2, page ->
                    command2Received.assertActivated()
                );
            }
        }
    }

    @Test
    void test_port_auto_selection() {
        Trigger command1Received = new Trigger();
        Trigger command2Received = new Trigger();
        try (PhotonPaw paw1 = new PhotonPaw().resourcesRoot("")) {
            try (PhotonPaw paw2 = new PhotonPaw().resourcesRoot("")) {
                paw1.handleCommand("a", msg -> {
                    assertEquals("ui command", msg);
                    command1Received.activate();
                }).start();
                paw2.handleCommand("a", msg -> {
                    assertEquals("ui command", msg);
                    command2Received.activate();
                }).start();

                assertTrue(paw1.getPort() < paw2.getPort());
                assertTrue(paw1.getWsPort() < paw2.getWsPort());

                withHtmlPage("/handle_ui_command.html", paw1.getPort(), page ->
                    command1Received.assertActivated()
                );
                withHtmlPage("/handle_ui_command.html", paw2.getPort(), page ->
                    command2Received.assertActivated()
                );
            }
        }
    }

    private PhotonPaw createBackend() {
        return new PhotonPaw().ports(port, port + 1).resourcesRoot("");
    }

    private void withHtmlPage(String path, Consumer<HtmlPage> x) {
        withHtmlPage(path, port, x);
    }

    private void withHtmlPage(String path, int customPort, Consumer<HtmlPage> x) {
        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            HtmlPage page = webClient.getPage("http://localhost:" + customPort + path);
            x.accept(page);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
