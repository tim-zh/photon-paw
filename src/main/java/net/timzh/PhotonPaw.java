package net.timzh;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class PhotonPaw implements AutoCloseable {
    private static final String MESSAGE_DELIMITER = "\n";
    private static final String ESCAPED_MESSAGE_DELIMITER = "\\n";

    private int port = 8080;
    private int wsPort = 8081;
    private String resourcesRoot;
    private Map<String, Consumer<String>> commandHandlers = new HashMap<>();
    private Map<String, Function<String, String>> queryHandlers = new HashMap<>();
    private BiConsumer<String, String> defaultHandler = (event, msg) -> {};
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

    public PhotonPaw ports(int http, int ws) {
        mustBeStarted(false);
        if (http == ws) {
            throw new RuntimeException("HTTP port must differ from WebSocket port");
        }
        port = http;
        wsPort = ws;
        return this;
    }

    public PhotonPaw resourcesRoot(String path) {
        mustBeStarted(false);
        resourcesRoot = path;
        return this;
    }

    public PhotonPaw handleCommand(String event, Consumer<String> handler) {
        mustBeStarted(false);
        commandHandlers.put(event, handler);
        return this;
    }

    public PhotonPaw handleQuery(String event, Function<String, String> handler) {
        mustBeStarted(false);
        queryHandlers.put(event, handler);
        return this;
    }

    public PhotonPaw defaultHandler(BiConsumer<String, String> handler) {
        mustBeStarted(false);
        defaultHandler = handler;
        return this;
    }

    public PhotonPaw bindPath(String path, String contentType, Supplier<String> response) {
        mustBeStarted(false);
        server.bindPath(path, contentType, response);
        return this;
    }

    private void send(String event, String correlationId, String message) {
        mustBeStarted(true);
        server.send(event + MESSAGE_DELIMITER + correlationId + MESSAGE_DELIMITER + message);
    }

    public void send(String event, String message) {
        send(event, "", message);
    }

    public PhotonPaw start() {
        return start(() -> {});
    }

    public PhotonPaw start(Runnable onStart) {
        mustBeStarted(false);
        started = true;
        server.bindPath("/photonpaw_client.js", "text/javascript", () ->
                readFile("photonpaw_client.js")
                        .replace("PORT", wsPort + "")
                        .replace("MESSAGE_DELIMITER", ESCAPED_MESSAGE_DELIMITER)
        );
        server.start(port, wsPort, resourcesRoot, msg -> {
            String[] parts = msg.split(MESSAGE_DELIMITER, 3);
            if (parts.length == 3) {
                String eventName = parts[0];
                String correlationId = parts[1];
                String data = parts[2];

                if (! correlationId.isEmpty() && queryHandlers.containsKey(eventName)) {
                    String result = queryHandlers.get(eventName).apply(data);
                    send(eventName, correlationId, result);
                } else {
                    commandHandlers.getOrDefault(eventName, x -> defaultHandler.accept("", x)).accept(data);
                }
            } else {
                defaultHandler.accept("", msg);
            }
        }, onStart);
        return this;
    }

    private static String readFile(String path) {
        InputStream stream = UndertowServer.class.getClassLoader().getResourceAsStream(path);
        Scanner s = new Scanner(stream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public PhotonPaw println(Object o) {
        System.out.println(o);
        return this;
    }

    public PhotonPaw openBrowser() {
        if (! GraphicsEnvironment.isHeadless()) {
            try {
                Desktop.getDesktop().browse(new URI("http://localhost:" + port + "/"));
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    public PhotonPaw waitForInput() {
        try {
            System.in.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public PhotonPaw stop() {
        mustBeStarted(true);
        server.stop();
        return this;
    }

    @Override
    public void close() {
        stop();
    }
}
