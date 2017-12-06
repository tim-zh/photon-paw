package tim.zh;

import fi.iki.elonen.NanoWSD;
import fi.iki.elonen.NanoWSD.WebSocketFrame.CloseCode;

import java.io.IOException;
import java.util.function.Consumer;

class NanoHttpdServer implements UiServer {

  private Server server;

  @Override
  public void start(String host, int port, int wsPort, String resourceRoot /*todo*/, Consumer<String> webSocketCallback) {
    server = new Server(host, port);
    server.callback = webSocketCallback;
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
  public void send(String wsMessage) {
    try {
      server.socket.send(wsMessage);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private class Server extends NanoWSD {

    Socket socket;
    Consumer<String> callback;

    Server(String host, int port) {
      super(host, port);
    }

    @Override
    protected Response serveHttp(IHTTPSession session) {
      return super.serveHttp(session);
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
