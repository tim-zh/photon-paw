package net.timzh;

import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.util.Headers;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.resource;
import static io.undertow.Handlers.websocket;

class UndertowServer implements UiServer {
    private Undertow server;
    private WebSocketChannel ws;
    private PathHandler handler = path();

    @Override
    public void start(int port, int wsPort, String resourceRoot, Consumer<String> wsCallback, Runnable onStart) {
        server = Undertow.builder()
                .addHttpListener(port, "localhost", handler
                        .addPrefixPath("/", resource(new FileResourceManager(new File(resourceRoot)))))
                .addHttpListener(wsPort, "localhost", path().addPrefixPath("/", websocket((exchange, channel) -> {
                    ws = channel;
                    ws.getReceiveSetter().set(new MyAbstractReceiveListener(wsCallback));
                    ws.resumeReceives();
                    onStart.run();
                })))
                .build();
        server.start();
    }

    @Override
    public void stop() {
        if (ws != null && !ws.isCloseFrameReceived()) {
            try {
                ws.sendClose();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        server.stop();
    }

    @Override
    public void send(String wsMessage) {
        if (ws == null) {
            throw new NullPointerException("websocket connection hasn't been established");
        }
        try {
            WebSockets.sendTextBlocking(wsMessage, ws);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void bindPath(String path, String contentType, Supplier<String> response) {
        handler.addPrefixPath(path, exchange -> {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType);
            exchange.getResponseSender().send(response.get());
        });
    }

    private static class MyAbstractReceiveListener extends AbstractReceiveListener {
        Consumer<String> wsCallback;

        MyAbstractReceiveListener(Consumer<String> wsCallback) {
            this.wsCallback = wsCallback;
        }

        @Override
        protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
            wsCallback.accept(message.getData());
        }
    }
}
