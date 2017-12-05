package tim.zh;

import fi.iki.elonen.NanoWSD;
import fi.iki.elonen.NanoWSD.WebSocketFrame.CloseCode;

import java.io.IOException;
import java.util.function.Consumer;

class NanoHttpdServer implements Server {

  private final Server server;
  private Consumer<String> callback;

  NanoHttpdServer(int port) {
    server = new Server(port);
  }

  @Override
  public void start() {
    try {
      server.start(Integer.MAX_VALUE);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void stop() {
    server.stop();
  }

  @Override
  public void send(String msg) {
    if (server.socket != null) {
      try {
        server.socket.send(msg);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void onMessage(Consumer<String> callback) {
    this.callback = callback;
  }

  @Override
  public void resourceRoot(String path) {
    //todo
  }

  private class Server extends NanoWSD {

    Socket socket;

    Server(int port) {
      super("localhost", port);
    }

    @Override
    protected WebSocket openWebSocket(IHTTPSession handshake) {
      socket = new Socket(handshake);
      return socket;
    }

    private class Socket extends WebSocket {

      Socket(IHTTPSession handshakeRequest) {
        super(handshakeRequest);
      }

      @Override
      protected void onOpen() {
      }

      @Override
      protected void onClose(CloseCode code, String reason, boolean initiatedByRemote) {
      }

      @Override
      protected void onMessage(WebSocketFrame message) {
        message.setUnmasked();
        callback.accept(message.getTextPayload());
      }

      @Override
      protected void onPong(WebSocketFrame pong) {
      }

      @Override
      protected void onException(IOException exception) {
      }
    }
  }
}
