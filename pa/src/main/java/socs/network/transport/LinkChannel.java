package socs.network.transport;

import socs.network.message.SOSPFPacket;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class LinkChannel {
  private final Socket socket;
  private final ObjectInputStream in;
  private final ObjectOutputStream out;

  public LinkChannel(Socket socket) throws IOException {
    this.socket = socket;

    this.out = new ObjectOutputStream(socket.getOutputStream());
    this.out.flush();

    this.in = new ObjectInputStream(socket.getInputStream());
  }

  public synchronized void send(SOSPFPacket p) throws IOException {
    out.writeObject(p);
    out.flush();
    out.reset();
  }

  public SOSPFPacket receive() throws IOException, ClassNotFoundException {
    try {
      return (SOSPFPacket) in.readObject();  // Blocking
    } catch (EOFException e) {
      // Remote side closed connection
      close();
      throw e;
    }
  }

  public void close() {
    try {
      in.close();
    } catch (IOException ignored) {
    }
    try {
      out.close();
    } catch (IOException ignored) {
    }
    try {
      socket.close();
    } catch (IOException ignored) {
    }
  }
}
