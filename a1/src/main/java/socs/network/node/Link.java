package socs.network.node;

import socs.network.message.SOSPFPacket;
import socs.network.transport.ErrorHandler;
import socs.network.transport.LinkChannel;
import socs.network.transport.PacketHandler;

import java.io.IOException;

public class Link {

  public RouterDescription ourRouter;
  public RouterDescription otherRouter;
  public int weight;

  public LinkChannel channel; // When establishing a link, need to open a socket for communication

  public Link(RouterDescription ourRouter, RouterDescription otherRouter, int weight, LinkChannel channel) {
    this.ourRouter = ourRouter;
    this.otherRouter = otherRouter;
    this.weight = weight;
    this.channel = channel;
  }

  public void listen(PacketHandler handler, ErrorHandler errorHandler) {
    new Thread(
            () -> {
              while (!Thread.currentThread().isInterrupted()) {
                try {
                  SOSPFPacket packet = channel.receive();
                  try {
                    handler.handle(packet, this.channel);
                  } catch (Exception e) {
                    errorHandler.handle(e);
                  }
                } catch (IOException | ClassNotFoundException e) {
                  errorHandler.handle(e);
                  break;
                }
              }
            }, "link-listener-" + otherRouter.simulatedIPAddress
    ).start();
  }

  public void delete() {
    this.channel.close();
  }

  @Override
  public String toString() {
    return "Link{" +
            "ourRouter=" + ourRouter.toString() +
            ", otherRouter=" + otherRouter.toString() +
            ", weight=" + weight +
            '}';
  }
}
