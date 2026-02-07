package socs.network.node;

import socs.network.message.SOSPFPacket;
import socs.network.transport.ErrorHandler;
import socs.network.transport.LinkChannel;
import socs.network.transport.PacketHandler;

import java.io.EOFException;
import java.io.IOException;

public class Link {

  public RouterDescription ourRouter;
  public RouterDescription otherRouter;
  public int weight;

  public LinkChannel channel; // When establishing a link, need to open a socket for communication
  public boolean helloInitiatedByMe;

  public Link(RouterDescription ourRouter, RouterDescription otherRouter, int weight, LinkChannel channel) {
    this.ourRouter = ourRouter;
    this.otherRouter = otherRouter;
    this.weight = weight;
    this.channel = channel;
    this.helloInitiatedByMe = false;
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

                } catch (EOFException e) {
                  // EOFException would imply that the socket closed on the other side
                  // ignore it for now, and just show it to the client
                  // TODO: as the assignment goes we can determine what behaviour we want for it

                  errorHandler.handle(e);
                  break;
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
