package tim.zh;

import io.undertow.Undertow;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.resource;
import static io.undertow.Handlers.websocket;

public class UndertowServer implements UiServer {
  private Undertow server;
  private WebSocketChannel ws;

  @Override
  public void start(String host, int port, int wsPort, String resourceRoot, Consumer<String> wsCallback) {
    server = Undertow.builder()
        .addHttpListener(port, host, path().addPrefixPath("/", resource(new FileResourceManager(new File(resourceRoot)))))
        .addHttpListener(wsPort, host, path().addPrefixPath("/", websocket((exchange, channel) -> {
          ws = channel;
          ws.getReceiveSetter().set(new MyAbstractReceiveListener(wsCallback));
          ws.resumeReceives();
        })))
        .build();
    server.start();
  }

  @Override
  public void stop() {
    server.stop();
  }

  @Override
  public void send(String wsMessage) {
    try {
      WebSockets.sendTextBlocking(wsMessage, ws);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static class MyAbstractReceiveListener extends AbstractReceiveListener {
    private final Consumer<String> wsCallback;

    public MyAbstractReceiveListener(Consumer<String> wsCallback) {
      this.wsCallback = wsCallback;
    }

    @Override
    protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
      wsCallback.accept(message.getData());
    }
  }
}
