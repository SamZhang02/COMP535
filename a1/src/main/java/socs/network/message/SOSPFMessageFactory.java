package socs.network.message;

import socs.network.node.RouterDescription;

import java.util.UUID;

public class SOSPFMessageFactory {
  public static SOSPFPacket createHello(RouterDescription srcRd, String dstSimulatedIp, String message) {
    return basePacket(srcRd, dstSimulatedIp)
            .setSospfType(SOSPFPacket.SOSPFType.HELLO)
            .setNeighborID(srcRd.simulatedIPAddress)
            .setRouterID(srcRd.simulatedIPAddress)
            .setMessage(message)
            .createSOSPFPacket();
  }

  public static SOSPFPacket createAttachResponse(RouterDescription srcRd, String dstSimulatedIp, boolean accepted) {
    return basePacket(srcRd, dstSimulatedIp)
            .setSospfType(SOSPFPacket.SOSPFType.HELLO)
            .setAccepted(accepted)
            .createSOSPFPacket();
  }

  private static SOSPFPacketBuilder basePacket(RouterDescription rd, String dstIP) {
    return new SOSPFPacketBuilder()
            .setRequestId(UUID.randomUUID().toString())
            .setSrcIP(rd.simulatedIPAddress)
            .setSrcProcessIP(rd.processIPAddress)
            .setSrcProcessPort(rd.processPortNumber)
            .setDstIP(dstIP);
  }

}

