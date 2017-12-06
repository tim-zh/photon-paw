package tim.zh;

import java.util.function.Consumer;

public interface UiServer {
  void start(String host, int port, String resourceRoot, Consumer<String> webSocketCallback);

  void stop();

  void send(String webSocketMessage);
}
