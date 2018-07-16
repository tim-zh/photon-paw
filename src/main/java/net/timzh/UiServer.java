package net.timzh;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface UiServer {
    void start(int port, int wsPort, String resourceRoot, Consumer<String> wsCallback, Runnable onStart);

    void stop();

    void send(String wsMessage);

    void bindPath(String path, String contentType, Supplier<String> response);
}
