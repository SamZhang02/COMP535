package socs.network.message;

import java.util.Vector;

public class SOSPFPacketBuilder {
    private String requestId;
    private String srcProcessIP;
    private short srcProcessPort;
    private String srcIP;
    private String dstIP;
    private SOSPFPacket.SOSPFType sospfType;
    private String routerID;
    private Boolean accepted = null;
    private String neighborID;
    private Vector<LSA> lsaArray;
    private String message;

    public SOSPFPacketBuilder setRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public SOSPFPacketBuilder setSrcProcessIP(String srcProcessIP) {
        this.srcProcessIP = srcProcessIP;
        return this;
    }

    public SOSPFPacketBuilder setSrcProcessPort(short srcProcessPort) {
        this.srcProcessPort = srcProcessPort;
        return this;
    }

    public SOSPFPacketBuilder setSrcIP(String srcIP) {
        this.srcIP = srcIP;
        return this;
    }

    public SOSPFPacketBuilder setDstIP(String dstIP) {
        this.dstIP = dstIP;
        return this;
    }

    public SOSPFPacketBuilder setSospfType(SOSPFPacket.SOSPFType sospfType) {
        this.sospfType = sospfType;
        return this;
    }

    public SOSPFPacketBuilder setRouterID(String routerID) {
        this.routerID = routerID;
        return this;
    }

    public SOSPFPacketBuilder setAccepted(boolean accepted) {
        this.accepted = accepted;
        return this;
    }

    public SOSPFPacketBuilder setNeighborID(String neighborID) {
        this.neighborID = neighborID;
        return this;
    }

    public SOSPFPacketBuilder setLsaArray(Vector<LSA> lsaArray) {
        this.lsaArray = lsaArray;
        return this;
    }

    public SOSPFPacketBuilder setMessage(String message) {
        this.message = message;
        return this;
    }

    public SOSPFPacket createSOSPFPacket() {
        return new SOSPFPacket(requestId, srcProcessIP, srcProcessPort, srcIP, dstIP, sospfType, routerID, accepted, neighborID, lsaArray, message);
    }
}