package socs.network.message;

import socs.network.node.RouterDescription;

import java.util.List;
import java.util.UUID;

public class SOSPFMessageFactory {

  public static SOSPFPacket createHello(RouterDescription srcRd, String dstSimulatedIp) {
    return createHello(srcRd, dstSimulatedIp, "");
  }

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

  public static SOSPFPacket createLSAUPDATE(RouterDescription srcRd, String dstSimulatedIp, List<LSA> lsa) {
    return basePacket(srcRd, dstSimulatedIp)
            .setSospfType(SOSPFPacket.SOSPFType.LINKSTATE_UPDATE)
            .setLsaArray(lsa)
            .createSOSPFPacket();
  }

  public static SOSPFPacket createMessagee(
          RouterDescription srcRd,
          String dstSimulatedIp,
          String message
  ) {
    return basePacket(srcRd, dstSimulatedIp)
            .setSospfType(SOSPFPacket.SOSPFType.APPLICATION_MSG)
            .setMessage(message)
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
