package tim.zh;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface UiServer {
  void start(String host, int port, int wsPort, String resourceRoot, Consumer<String> wsCallback);

  void stop();

  void send(String wsMessage);

  void bindPath(String path, String contentType, Supplier<String> response);
}
