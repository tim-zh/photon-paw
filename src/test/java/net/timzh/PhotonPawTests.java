package net.timzh;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PhotonPawTests {
    private int port = 19081;

    @Test
    void test_bindPath() {
        String testStr = "+";
        String testDoc = "<html><body>" + testStr + "</body></html>";
        try (PhotonPaw paw = createBackend()) {
            paw.bindPath("/bind", "text/html", () -> testDoc).start();
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

    private PhotonPaw createBackend() {
        return new PhotonPaw().ports(port, port + 1).resourcesRoot("./src/test/resources");
    }

    private void withHtmlPage(String path, Consumer<HtmlPage> x) {
        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            HtmlPage page = webClient.getPage("http://localhost:" + port + path);
            x.accept(page);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
