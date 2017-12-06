package tim.zh;

import io.undertow.Undertow;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

import java.io.File;
import java.util.function.Consumer;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.resource;
import static io.undertow.Handlers.websocket;

public class UndertowServer implements UiServer {
  private Undertow server;

  @Override
  public void start(String host, int port, int wsPort, String resourceRoot, Consumer<String> webSocketCallback) {
    server = Undertow.builder()
        .addHttpListener(port, host, path().addPrefixPath("/", resource(new FileResourceManager(new File(resourceRoot)))))
        .addHttpListener(wsPort, host, path().addPrefixPath("/", websocket((exchange, channel) -> {
          channel.getReceiveSetter().set(new AbstractReceiveListener() {
            @Override
            protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
              final String messageData = message.getData();
              WebSockets.sendText(messageData, channel, null);
            }
          });
          channel.resumeReceives();
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
    //todo
  }
}
