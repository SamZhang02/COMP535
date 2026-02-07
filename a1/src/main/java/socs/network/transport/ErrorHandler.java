package socs.network.transport;

@FunctionalInterface
public interface ErrorHandler {
  void handle(Exception e);
}
