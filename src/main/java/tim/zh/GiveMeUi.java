package tim.zh;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GiveMeUi {
    private static final String MESSAGE_DELIMITER = "\n";
    private static final String ESCAPED_MESSAGE_DELIMITER = "\\n";

    private int port = 8081;
    private int wsPort = 8082;
    private String resourcesRoot;
    private Map<String, Consumer<String>> handlers = new HashMap<>();
    private Consumer<String> defaultHandler = msg -> {};
    private UiServer server = createUiServer();
    private boolean started;

    private void mustBeStarted(boolean flag) {
        if (flag != started) {
            String must = flag ? "must" : "must not";
            throw new RuntimeException("Server " + must + " be started");
        }
    }

    protected UiServer createUiServer() {
        return new UndertowServer();
    }

    public GiveMeUi port(int number) {
        mustBeStarted(false);
        port = number;
        return this;
    }

    public GiveMeUi wsPort(int number) {
        mustBeStarted(false);
        wsPort = number;
        return this;
    }

    public GiveMeUi resourcesRoot(String path) {
        mustBeStarted(false);
        resourcesRoot = path;
        return this;
    }

    public GiveMeUi handler(String event, Consumer<String> handler) {
        mustBeStarted(false);
        handlers.put(event, handler);
        return this;
    }

    public GiveMeUi defaultHandler(Consumer<String> handler) {
        mustBeStarted(false);
        defaultHandler = handler;
        return this;
    }

    public GiveMeUi bindPath(String path, String contentType, Supplier<String> response) {
        mustBeStarted(false);
        server.bindPath(path, contentType, response);
        return this;
    }

    public void send(String event, String message) {
        mustBeStarted(true);
        server.send(event + MESSAGE_DELIMITER + message);
    }

    public GiveMeUi start() {
        mustBeStarted(false);
        started = true;
        server.bindPath("/givemeui_client.js", "text/javascript", () ->
                readFile("givemeui_client.js")
                        .replace("PORT", wsPort + "")
                        .replace("MESSAGE_DELIMITER", ESCAPED_MESSAGE_DELIMITER)
        );
        server.start(port, wsPort, resourcesRoot, msg -> {
            String[] parts = msg.split(MESSAGE_DELIMITER, 2);
            if (parts.length == 2) {
                handlers.getOrDefault(parts[0], defaultHandler).accept(parts[1]);
            } else {
                defaultHandler.accept(msg);
            }
        });
        return this;
    }

    private static String readFile(String path) {
        InputStream stream = UndertowServer.class.getClassLoader().getResourceAsStream(path);
        Scanner s = new Scanner(stream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public GiveMeUi println(Object o) {
        System.out.println(o);
        return this;
    }

    public GiveMeUi openBrowser() {
        if (! GraphicsEnvironment.isHeadless()) {
            try {
                Desktop.getDesktop().browse(new URI("http://localhost:" + port + "/"));
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    public GiveMeUi waitForInput() {
        try {
            System.in.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public GiveMeUi stop() {
        mustBeStarted(true);
        server.stop();
        return this;
    }
}
