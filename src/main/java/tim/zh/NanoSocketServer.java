package tim.zh;

import fi.iki.elonen.NanoWSD;
import fi.iki.elonen.NanoWSD.WebSocketFrame.CloseCode;

import java.io.IOException;
import java.util.function.Consumer;

class NanoSocketServer implements SocketServer {

  private final Wsd wsd;
  private Consumer<String> callback;

  public NanoSocketServer(int port) {
    wsd = new Wsd(port);
  }

  @Override
  public void start() {
    try {
      wsd.start();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void stop() {
    wsd.stop();
  }

  @Override
  public void send(String msg) {
    if (wsd.socket != null) {
      try {
        wsd.socket.send(msg);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void onMessage(Consumer<String> callback) {
    this.callback = callback;
  }

  private class Wsd extends NanoWSD {

    Socket socket;

    Wsd(int port) {
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
        /*try {
          sendFrame(message);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }*/
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
