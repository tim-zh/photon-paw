package net.timzh.photonpaw;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An easy and convenient lib to add HTML-based UI to a console app
 */
public class PhotonPaw {

    int port;
    int wsPort;
    String resourcesRoot;
    final Map<String, BiConsumer<String, PhotonPawStarted>> commandHandlers;
    final Map<String, Function<String, String>> queryHandlers;
    BiConsumer<String, PhotonPawStarted> defaultHandler;
    final Map<String, Function<UiHttpRequest, UiHttpResponse>> boundPaths = new HashMap<>();
    private boolean rootPathBound;

    public PhotonPaw() {
        port = -1;
        wsPort = -1;
        resourcesRoot = null;
        commandHandlers = new HashMap<>();
        queryHandlers = new HashMap<>();
        defaultHandler = (msg, out) -> {};
        rootPathBound = false;
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
    public PhotonPaw handleCommand(String event, BiConsumer<String, PhotonPawStarted> handler) {
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
        queryHandlers.put(event, handler);
        return this;
    }

    /**
     * Handler for all unprocessed messages
     *
     * @param handler default handler
     * @return this instance
     */
    public PhotonPaw defaultHandler(BiConsumer<String, PhotonPawStarted> handler) {
        defaultHandler = handler;
        return this;
    }

    /**
     * Configure custom response for a path
     *
     * @param path        url path to bind
     * @param response    response supplier
     * @return this instance
     */
    public PhotonPaw bindPath(String path, Function<UiHttpRequest, UiHttpResponse> response) {
        if (rootPathBound && "/".equals(path)) {
            throw new RuntimeException("Path \"/\" is already used by resourcesRoot");
        }
        rootPathBound = true;
        boundPaths.put(path, response);
        return this;
    }

    /**
     * Start the ui server
     *
     * @return this instance
     */
    public PhotonPawStarted start() {
        return start(x -> {});
    }

    /**
     * Start the ui server
     *
     * @param onStart callback to execute after establishing a connection with ui
     * @return this instance
     */
    public PhotonPawStarted start(Consumer<PhotonPawStarted> onStart) {
        return new PhotonPawStarted(this).start(onStart);
    }
}
