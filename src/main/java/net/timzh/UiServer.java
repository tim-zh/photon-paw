package net.timzh;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Interface for a backend that PhotonPaw can use, it is expected to support http, websockets, serving static resources
 */
public interface UiServer {
    /**
     * Start the ui server
     *
     * @param port         port to serve static resources
     * @param wsPort       port to use for websocket connection
     * @param resourceRoot path to a directory with static resources
     * @param wsCallback   callback that is called when a websocket message is received from the ui frontend
     * @param onStart      callback that is called when a websocket connection has been established with the ui frontend
     */
    void start(int port, int wsPort, String resourceRoot, Consumer<String> wsCallback, Runnable onStart);

    /**
     * Stop the ui server
     */
    void stop();

    /**
     * Send a websocket message to the ui frontend
     *
     * @param wsMessage the message
     */
    void send(String wsMessage);

    /**
     * Configure custom response for a path
     *
     * @param path        url path to bind
     * @param contentType response content type
     * @param response    response supplier
     */
    void bindPath(String path, String contentType, Supplier<String> response);
}
