package socs.network.message;

import java.io.Serializable;
import java.util.List;

public class SOSPFPacket implements Serializable {
  public String requestId;

  //for inter-process communication
  public String srcProcessIP;
  public short srcProcessPort;

  //simulated IP address
  public String srcIP;
  public String dstIP;

  //common header
  public SOSPFType sospfType; //0 - HELLO, 1 - LinkState Update, 2 - Application Message, 3 - Disconnect
  public String routerID;

  // null if not a response
  public Boolean accepted;

  //used by HELLO message to identify the sender of the message
  //e.g. when router A sends HELLO to its neighbor, it has to fill this field with its own
  //simulated IP address
  public String neighborID; //neighbor's simulated IP address

  //used by LSAUPDATE
  public List<LSA> lsaArray = null;

  //used by Application Message
  public String message; //user inputted message payload

  public SOSPFPacket(
          String requestId,
          String srcProcessIP,
          short srcProcessPort,
          String srcIP,
          String dstIP,
          SOSPFType sospfType,
          String routerID,
          Boolean accepted,
          String neighborID,
          List<LSA> lsaArray,
          String message
  ) {
    this.requestId = requestId;
    this.srcProcessIP = srcProcessIP;
    this.srcProcessPort = srcProcessPort;
    this.srcIP = srcIP;
    this.dstIP = dstIP;
    this.sospfType = sospfType;
    this.routerID = routerID;
    this.accepted = accepted;
    this.neighborID = neighborID;
    this.lsaArray = lsaArray;
    this.message = message;
  }

  public String displayString() {
    switch (this.sospfType) {
      case HELLO:
        return "HELLO";
      case LINKSTATE_UPDATE:
        return "LINKSTATE UPDATE";
      case DISCONNECT:
        return "DISCONNECT";
      case EXIT:
        return "EXIT";
      default:
        return this.message;
    }
  }

  @Override
  public String toString() {
    return "SOSPFPacket{" +
            "requestId='" + requestId + '\'' +
            ", srcProcessIP='" + srcProcessIP + '\'' +
            ", srcProcessPort=" + srcProcessPort +
            ", srcIP='" + srcIP + '\'' +
            ", dstIP='" + dstIP + '\'' +
            ", sospfType=" + sospfType +
            ", routerID='" + routerID + '\'' +
            ", accepted=" + accepted +
            ", neighborID='" + neighborID + '\'' +
            ", lsaArray=" + lsaArray +
            ", message='" + message + '\'' +
            '}';
  }

  public enum SOSPFType {
    HELLO((short) 0),
    LINKSTATE_UPDATE((short) 1),
    APPLICATION_MSG((short) 2),
    EXIT((short) 3),
    DISCONNECT((short) 4);

    SOSPFType(short k) {
    }
  }
}
