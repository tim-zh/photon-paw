package tim.zh;

import java.io.IOException;

public class GiveMeUi {
  public static void main(String[] args) {
    int port = 8080;
    SocketServer server = new NanoSocketServer(port);
    server.onMessage(msg -> {
      System.out.println(msg);
      server.send(msg+"!");
    });
    server.start();
    System.out.println("Server started, hit Enter to stop.\n");
    try {
      System.in.read();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    server.send("321!");
    server.stop();
    System.out.println("Server stopped.\n");
  }
}
