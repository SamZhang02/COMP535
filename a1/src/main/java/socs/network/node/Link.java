package socs.network.node;

import socs.network.message.SOSPFPacket;
import socs.network.transport.LinkChannel;

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

  public void listen(PacketHandler handler) {
    new Thread(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          SOSPFPacket packet = channel.receive();
          handler.handle(packet);
        } catch (IOException | ClassNotFoundException e) {
          break;
        }
      }
    }, "link-listener-" + otherRouter.simulatedIPAddress).start();
  }

  public void delete() {
    this.channel.close();
  }

  @Override
  public String toString() {
    return "Link{" +
            "ourRouter=" + ourRouter +
            ", otherRouter=" + otherRouter +
            ", weight=" + weight +
            '}';
  }
}
