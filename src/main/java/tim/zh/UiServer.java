package tim.zh;

import java.util.function.Consumer;

public interface UiServer {
  void start(String host, int port, int wsPort, String resourceRoot, Consumer<String> wsCallback);

  void stop();

  void send(String wsMessage);
}
