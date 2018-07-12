package net.timzh;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class PhotonPawTests {
    private int port = 19081;
    private int wsPort = 19082;

    @Test
    void test_bindPath() throws Exception {
        String testDoc = "<html><body>+</body></html>";
        try (PhotonPaw paw = createBackend()) {
            paw
                    .bindPath("/bind", "text/html", () -> testDoc)
                    .start();
            withHtmlPage("/bind", page ->
                    assertEquals("+", page.asText())
            );
        }
    }

    @Test
    void test_handleCommand() throws Exception {
        Trigger handleCommandTrigger = new Trigger();
        try (PhotonPaw paw = createBackend()) {
            paw
                    .handleCommand("a", msg -> {
                        assertEquals("frontend command", msg);
                        handleCommandTrigger.activate();
                    })
                    .start();
            withHtmlPage("/handleCommand.html", page ->
                handleCommandTrigger.assertActivated()
            );
        }
    }

    private PhotonPaw createBackend() {
        return new PhotonPaw().ports(port, wsPort).resourcesRoot("./src/test/resources");
    }

    private void withHtmlPage(String path, Consumer<HtmlPage> x) throws Exception {
        try (WebClient webClient = new WebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:" + port + path);
            x.accept(page);
        }
    }
}
