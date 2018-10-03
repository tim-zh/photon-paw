package net.timzh.photonpaw;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

/**
 * An easy and convenient lib to add HTML-based UI to a console app
 */
public class PhotonPaw implements AutoCloseable {
    private static final int START_PORT = 19080;

    protected String messagePartsDelimiter;
    protected String unloadEvent;

    private final Log log;

    private int port;
    private int wsPort;
    private String resourcesRoot;
    private final Map<String, Consumer<String>> commandHandlers;
    private final Map<String, Function<String, String>> queryHandlers;
    private BiConsumer<String, String> defaultHandler;

    private final UiServer server;

    private boolean rootPathBound;
    private boolean started;
    private final Object unloadWaitLock;
    private final Object interruptionWaitLock;

    public PhotonPaw() {
        messagePartsDelimiter = "\n";
        unloadEvent = "__unload__";

        log = LogFactory.getLog(PhotonPaw.class);

        port = -1;
        wsPort = -1;
        resourcesRoot = null;
        commandHandlers = new HashMap<>();
        queryHandlers = new HashMap<>();
        defaultHandler = (event, msg) -> {};

        rootPathBound = false;
        started = false;
        unloadWaitLock = new Object();
        interruptionWaitLock = new Object();

        log.info("creating ui server");
        server = createUiServer();
    }

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

    private static int firstAvailablePort(int startFrom) {
        while (! isAvailable(startFrom)) {
            ++startFrom;
        }
        return startFrom;
    }

    private static boolean isAvailable(int portToCheck) {
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
     * Get ui server state
     *
     * @return true when {@link PhotonPaw#start()} has been invoked
     */
    public boolean isStarted() {
        return started;
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
     * @param path path to a directory with static resources (see {@link ClassLoader#getResource(String)})
     * @return this instance
     */
    public PhotonPaw resourcesRoot(String path) {
        mustBeStarted(false);
        if (rootPathBound) {
            throw new RuntimeException("Path \"/\" is already used by bindPath");
        }
        rootPathBound = true;
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
    public PhotonPaw bindPath(String path, String contentType, Function<UiHttpRequest, String> response) {
        mustBeStarted(false);
        if (rootPathBound && "/".equals(path)) {
            throw new RuntimeException("Path \"/\" is already used by resourcesRoot");
        }
        rootPathBound = true;
        server.bindPath(path, contentType, response);
        return this;
    }

    private void send(String event, String correlationId, String message) {
        mustBeStarted(true);
        String msg = event + messagePartsDelimiter + correlationId + messagePartsDelimiter + message;
        log.info("sending message:\n" + msg);
        server.send(msg);
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
        handleCommand(unloadEvent, x -> {
            log.info("processing unload");
            synchronized (unloadWaitLock) {
                unloadWaitLock.notifyAll();
            }
        });
        started = true;
        if (port == -1) {
            port = firstAvailablePort(START_PORT);
        }
        if (wsPort == -1) {
            wsPort = firstAvailablePort(port + 1);
        }
        String jsClient = readFile("photonpaw_client.js")
            .replace("PORT", wsPort + "")
            .replace("MESSAGE_PARTS_DELIMITER", StringEscapeUtils.escapeJava(messagePartsDelimiter))
            .replace("UNLOAD_EVENT", unloadEvent);
        server.bindPath("/photonpaw_client.js", "application/javascript", request -> {
            log.info("sending js client");
            return jsClient;
        });
        log.info("starting, port=" + port + ", wsPort=" + wsPort);
        server.start(port, wsPort, resourcesRoot, msg -> {
            String[] parts = msg.split(messagePartsDelimiter, 3);
            if (parts.length == 3) {
                String eventName = parts[0];
                String correlationId = parts[1];
                String data = parts[2];

                if (! correlationId.isEmpty() && queryHandlers.containsKey(eventName)) {
                    log.info("processing query:\n" + msg);
                    String result = queryHandlers.get(eventName).apply(data);
                    send(eventName, correlationId, result);
                } else {
                    log.info("processing " +
                        (commandHandlers.containsKey(eventName) ? "command:\n" : "unknown command:\n") + msg);
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
     * Convenient way to call {@link System#out}'s {@link java.io.PrintStream#println(Object) println}
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
     * @param path path to open, starting with {@code /}
     * @return this instance
     */
    public PhotonPaw openBrowser(String path) {
        mustBeStarted(true);
        if (! GraphicsEnvironment.isHeadless()) {
            try {
                log.info("opening browser");
                Desktop.getDesktop().browse(new URI("http://localhost:" + port + path));
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    /**
     * Wait for any input from {@link System#in}
     *
     * @return this instance
     */
    public PhotonPaw waitForInput() {
        try {
            log.info("waiting for input");
            System.in.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * Wait for {@link PhotonPaw#unloadEvent} from ui (sent when the tab is closed or refreshed)
     *
     * @return this instance
     */
    public PhotonPaw waitForTabToUnload() {
        synchronized (unloadWaitLock) {
            try {
                log.info("waiting for tab unload");
                unloadWaitLock.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    /**
     * Wait for {@link PhotonPaw#interrupt()}
     *
     * @return this instance
     */
    public PhotonPaw waitForInterruption() {
        synchronized (interruptionWaitLock) {
            try {
                log.info("waiting for interruption");
                interruptionWaitLock.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    /**
     * Interrupt {@link PhotonPaw#waitForInterruption()}
     */
    public void interrupt() {
        synchronized (interruptionWaitLock) {
            log.info("interrupting");
            interruptionWaitLock.notifyAll();
        }
    }

    /**
     * Stop the ui server
     *
     * @return this instance
     */
    public PhotonPaw stop() {
        mustBeStarted(true);
        log.info("stopping");
        server.stop();
        return this;
    }

    @Override
    public void close() {
        stop();
    }
}
