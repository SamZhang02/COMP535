package socs.network.transport;

import java.io.IOException;
import java.net.Socket;

@FunctionalInterface
public interface RequestHandler {
  void handle(Socket socket) throws IOException;
}
