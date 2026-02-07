package socs.network.node;

import socs.network.message.SOSPFPacket;

@FunctionalInterface
public interface PacketHandler {
  void handle(SOSPFPacket packet);
}
