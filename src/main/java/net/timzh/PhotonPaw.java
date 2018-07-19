package net.timzh;

import org.apache.commons.text.StringEscapeUtils;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An easy and convenient way to add HTML-based UI to a console app
 */
public class PhotonPaw implements AutoCloseable {
    private static final String MESSAGE_PARTS_DELIMITER = "\n";
    private static final int START_PORT = 19080;

    private int port = -1;
    private int wsPort = -1;
    private String resourcesRoot;
    private Map<String, Consumer<String>> commandHandlers = new HashMap<>();
    private Map<String, Function<String, String>> queryHandlers = new HashMap<>();
    private BiConsumer<String, String> defaultHandler = (event, msg) -> {};
    private UiServer server = createUiServer();
    private boolean rootPathBound;
    private boolean started;

    /**
     * Main port
     *
     * @return main port
     */
    public int getPort() {
        return port;
    }

    /**
     * Websocket port
     *
     * @return websocket port
     */
    public int getWsPort() {
        return wsPort;
    }

    private int firstAvailablePort(int startFrom) {
        while (! isAvailable(startFrom)) {
            ++startFrom;
        }
        return startFrom;
    }

    private boolean isAvailable(int portToCheck) {
        try (ServerSocket ss = new ServerSocket(portToCheck); DatagramSocket ds = new DatagramSocket(portToCheck)) {
            ss.setReuseAddress(true);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private void mustBeStarted(boolean flag) {
        if (flag != started) {
            String must = flag ? "must" : "must not";
            throw new RuntimeException("Server " + must + " be started");
        }
    }

    /**
     * Override this to use different underlying server
     *
     * @return ui server instance
     */
    protected UiServer createUiServer() {
        return new UndertowServer();
    }

    /**
     * Configure ports, {@code -1} (default value) to autoselect
     *
     * @param http main http port
     * @param ws   websocket port
     * @return this instance
     */
    public PhotonPaw ports(int http, int ws) {
        mustBeStarted(false);
        if (http == ws) {
            throw new RuntimeException("HTTP port must differ from WebSocket port");
        }
        port = http;
        wsPort = ws;
        return this;
    }

    /**
     * Configure static resources location
     *
     * @param path path to a directory with static resources
     * @return this instance
     */
    public PhotonPaw resourcesRoot(String path) {
        mustBeStarted(false);
        if (rootPathBound && path.equals("/")) {
            throw new RuntimeException("Path \"/\" is already used by bindPath");
        }
        resourcesRoot = path;
        return this;
    }

    /**
     * Add a handler for commands from ui by name
     *
     * @param event   event name
     * @param handler command handler
     * @return this instance
     */
    public PhotonPaw handleCommand(String event, Consumer<String> handler) {
        mustBeStarted(false);
        commandHandlers.put(event, handler);
        return this;
    }

    /**
     * Add a handler for commands from ui that expect some response
     *
     * @param event   event name
     * @param handler query handler
     * @return this instance
     */
    public PhotonPaw handleQuery(String event, Function<String, String> handler) {
        mustBeStarted(false);
        queryHandlers.put(event, handler);
        return this;
    }

    /**
     * Handler for all unprocessed messages
     *
     * @param handler default handler
     * @return this instance
     */
    public PhotonPaw defaultHandler(BiConsumer<String, String> handler) {
        mustBeStarted(false);
        defaultHandler = handler;
        return this;
    }

    /**
     * Configure custom response for a path
     *
     * @param path        url path to bind
     * @param contentType response content type
     * @param response    response supplier
     * @return this instance
     */
    public PhotonPaw bindPath(String path, String contentType, Supplier<String> response) {
        mustBeStarted(false);
        if (path.equals("/")) {
            if (resourcesRoot != null) {
                throw new RuntimeException("Path \"/\" is already used by resourcesRoot");
            } else {
                rootPathBound = true;
            }
        }
        server.bindPath(path, contentType, response);
        return this;
    }

    private void send(String event, String correlationId, String message) {
        mustBeStarted(true);
        server.send(event + MESSAGE_PARTS_DELIMITER + correlationId + MESSAGE_PARTS_DELIMITER + message);
    }

    /**
     * Send an event to ui
     *
     * @param event   event name
     * @param message event body
     */
    public void send(String event, String message) {
        send(event, "", message);
    }

    /**
     * Start the ui server
     *
     * @return this instance
     */
    public PhotonPaw start() {
        return start(() -> {});
    }

    /**
     * Start the ui server
     *
     * @param onStart callback to execute after establishing a connection with ui
     * @return this instance
     */
    public PhotonPaw start(Runnable onStart) {
        mustBeStarted(false);
        started = true;
        server.bindPath("/photonpaw_client.js", "application/javascript", () ->
                readFile("photonpaw_client.js")
                        .replace("PORT", wsPort + "")
                        .replace("MESSAGE_PARTS_DELIMITER", StringEscapeUtils.escapeJava(MESSAGE_PARTS_DELIMITER))
        );
        if (port == -1) {
            port = firstAvailablePort(START_PORT);
        }
        if (wsPort == -1) {
            wsPort = firstAvailablePort(port + 1);
        }
        server.start(port, wsPort, resourcesRoot, msg -> {
            String[] parts = msg.split(MESSAGE_PARTS_DELIMITER, 3);
            if (parts.length == 3) {
                String eventName = parts[0];
                String correlationId = parts[1];
                String data = parts[2];

                if (! correlationId.isEmpty() && queryHandlers.containsKey(eventName)) {
                    String result = queryHandlers.get(eventName).apply(data);
                    send(eventName, correlationId, result);
                } else {
                    commandHandlers.getOrDefault(eventName, x -> defaultHandler.accept(eventName, x)).accept(data);
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

    /**
     * Convenient way to call {@code System.out.println}
     *
     * @param o object to print
     * @return this instance
     */
    public PhotonPaw println(Object o) {
        System.out.println(o);
        return this;
    }

    /**
     * Open system default browser
     *
     * @return this instance
     */
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

    /**
     * Wait for any input from {@code System.in}
     *
     * @return this instance
     */
    public PhotonPaw waitForInput() {
        try {
            System.in.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * Stop the ui server
     *
     * @return this instance
     */
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
