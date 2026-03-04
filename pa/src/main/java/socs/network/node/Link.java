package socs.network.node;

import socs.network.message.SOSPFPacket;
import socs.network.transport.ErrorHandler;
import socs.network.transport.LinkChannel;
import socs.network.transport.PacketHandler;

import java.io.EOFException;
import java.io.IOException;

/**
 * A Link maintains information of which router our router is attached to;
 * For each router attached to, we keep an open socket maintained through {@link LinkChannel}
 * with them for frequent communication.
 */
public class Link {

  public RouterDescription ourRouter;
  public RouterDescription otherRouter;
  public int weight;

  public LinkChannel channel;
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
                  // Maintain neighbour in ports table but drop adjacency state.
                  this.markDisconnected();

                  errorHandler.handle(e);
                  break;
                } catch (IOException | ClassNotFoundException e) {
                  markDisconnected();
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

  private void markDisconnected() {
    if (this.otherRouter != null) {
      this.otherRouter.status = null;
    }
    this.helloInitiatedByMe = false;
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
