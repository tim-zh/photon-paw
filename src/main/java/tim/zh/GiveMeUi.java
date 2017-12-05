package tim.zh;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class GiveMeUi {
  private int port = 8080;
  private String resourcesRoot;
  private Server server;
  private Map<String, Consumer<String>> handlers = new HashMap<>();

  public GiveMeUi port(int port) {
    this.port = port;
    return this;
  }

  public GiveMeUi resourcesRoot(String resourcesRoot) {
    this.resourcesRoot = resourcesRoot;
    return this;
  }

  public GiveMeUi handler(String event, Consumer<String> handler) {
    handlers.put(event, handler);
    return this;
  }

  public void send(String event, String message) {
    server.send(event + '\n' + message);
  }

  public GiveMeUi start() {
    server = new NanoHttpdServer(port);
    server.resourceRoot(resourcesRoot);
    server.onMessage(msg -> {
      String[] parts = msg.split("\n");
      if (parts.length != 2) {
        throw new RuntimeException("invalid message " + msg);
      }
      handlers.getOrDefault(parts[0], m -> {/*todo*/}).accept(parts[1]);
    });
    server.start();
    return this;
  }

  public GiveMeUi println(Object o) {
    System.out.println(o);
    return this;
  }

  public GiveMeUi openBrowser() {
    if (! GraphicsEnvironment.isHeadless()) {
      try {
        Desktop.getDesktop().browse(new URI("http://localhost:" + port + "/"));
      } catch (IOException | URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }
    return this;
  }

  public GiveMeUi waitForInput() {
    try {
      System.in.read();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  public GiveMeUi stop() {
    server.stop();
    return this;
  }
}
