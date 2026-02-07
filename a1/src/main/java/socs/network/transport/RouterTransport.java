package socs.network.transport;

import socs.network.util.Configuration;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

// Server thread to receive communications
public class RouterTransport {
  private final ServerSocket serverSocket;

  public RouterTransport(Configuration config) throws IOException {
    this.serverSocket = new ServerSocket(config.getShort("socs.network.router.port"));
  }

  public Thread serve(RequestHandler handler) {
    Thread t = new Thread(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          Socket socket = serverSocket.accept();
          new Thread(() -> {
            try {
              handle(socket, handler);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }).start();
        } catch (IOException e) {
          break;
        }
      }
    });
    t.setDaemon(true);
    t.start();
    return t;
  }

  private void handle(Socket socket, RequestHandler handler) throws IOException {
    handler.handle(socket);
  }
}
