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
  private static final String MESSAGE_DELIMITER = "\n";
  private static final String HOST = "localhost";

  private int port = 8081;
  private int wsPort = 8082;
  private String resourcesRoot;
  private Map<String, Consumer<String>> handlers = new HashMap<>();
  private Consumer<String> defaultHandler = msg -> {};
  private UiServer server;
  private boolean started;

  private void shouldBeStarted(boolean flag) {
    if (flag != started) {
      throw new RuntimeException();
    }
  }

  protected UiServer createUiServer() {
    return new UndertowServer();
  }

  public GiveMeUi port(int number) {
    shouldBeStarted(false);
    port = number;
    return this;
  }

  public GiveMeUi wsPort(int number) {
    shouldBeStarted(false);
    wsPort = number;
    return this;
  }

  public GiveMeUi resourcesRoot(String path) {
    shouldBeStarted(false);
    resourcesRoot = path;
    return this;
  }

  public GiveMeUi handler(String event, Consumer<String> handler) {
    shouldBeStarted(false);
    handlers.put(event, handler);
    return this;
  }

  public GiveMeUi defaultHandler(Consumer<String> handler) {
    shouldBeStarted(false);
    defaultHandler = handler;
    return this;
  }

  public void send(String event, String message) {
    shouldBeStarted(true);
    server.send(event + MESSAGE_DELIMITER + message);
  }

  public GiveMeUi start() {
    shouldBeStarted(false);
    started = true;
    server = createUiServer();
    server.start(HOST, port, wsPort, resourcesRoot, msg -> {
      String[] parts = msg.split(MESSAGE_DELIMITER, 2);
      if (parts.length == 2) {
        handlers.getOrDefault(parts[0], defaultHandler).accept(parts[1]);
      } else {
        defaultHandler.accept(msg);
      }
    });
    return this;
  }

  public GiveMeUi println(Object o) {
    System.out.println(o);
    return this;
  }

  public GiveMeUi openBrowser() {
    if (! GraphicsEnvironment.isHeadless()) {
      try {
        Desktop.getDesktop().browse(new URI("http://" + HOST + ":" + port + "/"));
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
    shouldBeStarted(true);
    server.stop();
    return this;
  }
}
