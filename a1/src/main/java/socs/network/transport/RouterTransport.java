package socs.network.transport;

import socs.network.message.SOSPFPacket;
import socs.network.util.Configuration;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

// Communication between routers should use RouterTransport
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

  public SOSPFPacket sendAndWait(String ip, int port, SOSPFPacket msg)
          throws IOException, ClassNotFoundException {

    try (Socket socket = new Socket(ip, port);
         ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
         ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

      out.writeObject(msg);
      out.flush();
      return (SOSPFPacket) in.readObject();
    }
  }

  public void send(String ip, int port, SOSPFPacket msg) {
    new Thread(() -> {
      try (Socket socket = new Socket(ip, port);
           ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

        out.writeObject(msg);
        out.flush();
      } catch (IOException ignored) {
      }
    }).start();
  }

  private void handle(Socket socket, RequestHandler handler) throws IOException {
    handler.handle(socket);
  }
}
