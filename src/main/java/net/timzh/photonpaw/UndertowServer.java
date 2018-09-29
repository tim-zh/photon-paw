package net.timzh.photonpaw;

import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.util.Headers;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.resource;
import static io.undertow.Handlers.websocket;

class UndertowServer implements UiServer {
    private Undertow server;
    private WebSocketChannel ws;
    private PathHandler handler = path();

    @Override
    public void start(int port, int wsPort, String resourceRoot, Consumer<String> wsCallback, Runnable onStart) {
        Builder builder = Undertow.builder();
        if (! resourceRoot.isEmpty()) {
            FileResourceManager fileManager = new FileResourceManager(new File(resourceRoot));
            handler.addPrefixPath("/", resource(fileManager));
        }
        builder.addHttpListener(port, "localhost", handler);
        WebSocketConnectionCallback callback = (exchange, channel) -> {
            ws = channel;
            ws.getReceiveSetter().set(new MyAbstractReceiveListener(wsCallback));
            ws.resumeReceives();
            onStart.run();
        };
        server = builder
                .addHttpListener(wsPort, "localhost", path().addPrefixPath("/", websocket(callback)))
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
    public void bindPath(String path, String contentType, Function<UiHttpRequest, String> response) {
        handler.addPrefixPath(path, exchange -> {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType);
            exchange.getResponseSender().send(response.apply(requestFrom(exchange)));
        });
    }

    private static UiHttpRequest requestFrom(HttpServerExchange exchange) {
        Map<String, List<String>> params = new HashMap<>();
        exchange.getQueryParameters().forEach((key, value) -> params.put(key, new ArrayList<>(value)));
        Map<String, List<String>> headers = new HashMap<>();
        exchange.getRequestHeaders().forEach(header -> headers.put(header.getHeaderName().toString(), header));
        return new UiHttpRequest(
                exchange.getRequestMethod().toString(),
                exchange.getProtocol().toString(),
                exchange.getHostName(),
                exchange.getHostPort(),
                params,
                headers);
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
