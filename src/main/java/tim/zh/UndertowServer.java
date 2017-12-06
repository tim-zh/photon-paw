package tim.zh;

import io.undertow.Undertow;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.util.Headers;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
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
        .addHttpListener(port, host, path()
            .addPrefixPath("/givemeui_client.js", exchange -> {
              exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/javascript");
              String response = readFile("givemeui_client.js")
                  .replace("HOST", host)
                  .replace("PORT", wsPort + "");
              exchange.getResponseSender().send(response);
            })
            .addPrefixPath("/", resource(new FileResourceManager(new File(resourceRoot)))))
        .addHttpListener(wsPort, host, path().addPrefixPath("/", websocket((exchange, channel) -> {
          ws = channel;
          ws.getReceiveSetter().set(new MyAbstractReceiveListener(wsCallback));
          ws.resumeReceives();
        })))
        .build();
    server.start();
  }

  private String readFile(String path) {
    InputStream stream = UndertowServer.class.getClassLoader().getResourceAsStream(path);
    Scanner s = new Scanner(stream).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
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
