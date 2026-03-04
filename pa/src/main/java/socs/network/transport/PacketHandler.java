package socs.network.transport;

import socs.network.message.SOSPFPacket;

@FunctionalInterface
public interface PacketHandler {
  void handle(SOSPFPacket packet, LinkChannel ch);
}
