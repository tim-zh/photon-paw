package tim.zh;

import java.util.function.Consumer;

public interface SocketServer {
  void start();

  void stop();

  void send(String msg);

  void onMessage(Consumer<String> callback);
}
