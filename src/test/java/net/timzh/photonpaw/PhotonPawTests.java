package net.timzh.photonpaw;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class PhotonPawTests {

    private static final int PORT = 19081;

    @Test
    void test_bind_path() {
        String testStr = "+";
        String testDoc = "<html><body>" + testStr + "</body></html>";
        PhotonPaw paw = createBackend().bindPath("/bind", request -> UiHttpResponse.of("text/html", testDoc));
        try (PhotonPawStarted x = paw.start()) {
            withHtmlPage("/bind", page ->
                    assertEquals(testStr, page.asText())
            );
        }
    }

    @Test
    void test_handle_ui_command() {
        Trigger commandReceived = new Trigger();
        PhotonPaw paw = createBackend().handleCommand("a", (msg, out) -> {
            assertEquals("ui command", msg);
            commandReceived.activate();
        });
        try (PhotonPawStarted x = paw.start()) {
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
        PhotonPaw paw = createBackend().handleCommand("b", (msg, out) -> {
            assertEquals(testMsg, msg);
            commandReceived.activate();
        });
        try (PhotonPawStarted x = paw.start(out -> {
            out.send("a", testMsg);
            commandSent.activate();
        })) {
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
        PhotonPaw paw = createBackend().handleQuery("a", msg -> {
            assertEquals(testMsg, msg);
            queryReceived.activate();
            return msg;
        }).handleCommand("b", (msg, out) -> {
            assertEquals(testMsg, msg);
            queryResponseAccepted.activate();
        });
        try (PhotonPawStarted x = paw.start()) {
            withHtmlPage("/handle_ui_query.html", page -> {
                queryReceived.assertActivated();
                queryResponseAccepted.assertActivated();
            });
        }
    }

    @Test
    void test_server_default_handler() {
        Trigger commandReceived = new Trigger();
        PhotonPaw paw = createBackend().defaultHandler((msg, out) -> {
            assertTrue(msg.startsWith("a"));
            assertTrue(msg.endsWith("ui command"));
            commandReceived.activate();
        });
        try (PhotonPawStarted x = paw.start()) {
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
        PhotonPaw paw = createBackend().handleCommand("b", (msg, out) -> {
            assertEquals(testEvent + testMsg, msg);
            commandReceived.activate();
        });
        try (PhotonPawStarted x = paw.start(out -> {
            out.send(testEvent, testMsg);
            commandSent.activate();
        })) {
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
        PhotonPaw paw = new PhotonPaw()
                .bindPath("/bind", request -> UiHttpResponse.of("text/html", testDoc))
                .handleCommand("a", (msg, out) -> {
                    assertEquals("ui command", msg);
                    commandReceived.activate();
                });
        try (PhotonPawStarted x = paw.start()) {
            withHtmlPage("/bind", x.getPort(), page ->
                    commandReceived.assertActivated()
            );
        }
    }

    @Test
    void test_multiple_instances() {
        Trigger command1Received = new Trigger();
        Trigger command2Received = new Trigger();
        PhotonPaw paw1 = new PhotonPaw().ports(PORT, PORT + 1).resourcesRoot("").handleCommand("a", (msg, out) -> {
            assertEquals("ui command", msg);
            command1Received.activate();
        });
        PhotonPaw paw2 = new PhotonPaw().ports(PORT + 2, PORT + 3).resourcesRoot("").handleCommand("a", (msg, out) -> {
            assertEquals("ui command", msg);
            command2Received.activate();
        });
        try (PhotonPawStarted x = paw1.start()) {
            try (PhotonPawStarted y = paw2.start()) {
                withHtmlPage("/handle_ui_command.html", PORT, page ->
                        command1Received.assertActivated()
                );
                withHtmlPage("/handle_ui_command.html", PORT + 2, page ->
                        command2Received.assertActivated()
                );
            }
        }
    }

    @Test
    void test_port_auto_selection() {
        Trigger command1Received = new Trigger();
        Trigger command2Received = new Trigger();
        PhotonPaw paw1 = new PhotonPaw().resourcesRoot("").handleCommand("a", (msg, out) -> {
            assertEquals("ui command", msg);
            command1Received.activate();
        });
        PhotonPaw paw2 = new PhotonPaw().resourcesRoot("").handleCommand("a", (msg, out) -> {
            assertEquals("ui command", msg);
            command2Received.activate();
        });
        try (PhotonPawStarted x = paw1.start()) {
            try (PhotonPawStarted y = paw2.start()) {
                assertTrue(x.getPort() < y.getPort());
                assertTrue(x.getWsPort() < y.getWsPort());

                withHtmlPage("/handle_ui_command.html", x.getPort(), page ->
                        command1Received.assertActivated()
                );
                withHtmlPage("/handle_ui_command.html", y.getPort(), page ->
                        command2Received.assertActivated()
                );
            }
        }
    }

    private PhotonPaw createBackend() {
        return new PhotonPaw().ports(PORT, PORT + 1).resourcesRoot("");
    }

    private void withHtmlPage(String path, Consumer<HtmlPage> x) {
        withHtmlPage(path, PORT, x);
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
