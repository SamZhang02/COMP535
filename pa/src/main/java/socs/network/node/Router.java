package socs.network.node;

import socs.network.cli.Console;
import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFMessageFactory;
import socs.network.message.SOSPFPacket;
import socs.network.node.ports.PortsTable;
import socs.network.transport.ErrorHandler;
import socs.network.transport.LinkChannel;
import socs.network.transport.PacketHandler;
import socs.network.transport.RouterTransport;
import socs.network.util.Configuration;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Router {
  RouterDescription rd = new RouterDescription();
  protected LinkStateDatabase lsd;

  private final Console console;
  private final RouterTransport routerTransport;
  private final PortsTable portsTable;
  private final ErrorHandler errorHandler;
  private Thread clientServiceThread;


  public Router(Configuration config, RouterTransport rt, Console console, PortsTable portsTable) {
    rd.processIPAddress = config.getString("socs.network.router.pip");
    rd.processPortNumber = config.getShort("socs.network.router.port");
    rd.simulatedIPAddress = config.getString("socs.network.router.ip");

    lsd = new LinkStateDatabase(rd);

    routerTransport = rt;
    this.console = console;
    this.portsTable = portsTable;
    this.errorHandler = (e) -> {
      e.printStackTrace();
      console.log("");
    };
  }

  public synchronized void start() {
    this.rd.print();

    if (clientServiceThread != null && clientServiceThread.isAlive()) {
      return;
    }

    clientServiceThread = this.routerTransport.serve(this::requestHandler, this.errorHandler);
  }

  public synchronized void stop() {
    Thread thread = clientServiceThread;
    if (thread != null) {
      thread.interrupt();
      clientServiceThread = null;
    }
  }


  /**
   * output the shortest path to the given destination ip
   * <p/>
   * format: source ip address  -> ip address -> ... -> destination ip
   *
   * @param destinationIP the ip adderss of the destination simulated router
   */
  public void processDetect(String destinationIP) {
    List<String> path = lsd.getShortestPath(destinationIP);
    if (path != null) {
      String pathString = String.join(" -> ", path);
      console.log("Path found: " + pathString);
    } else {
      console.log("Path not found");
    }
  }

  /**
   * disconnect with the router identified by the given destination ip address
   * Notice: this command should trigger the synchronization of database
   *
   * @param portNumber the port number which the link attaches at
   */
  public void processDisconnect(short portNumber) {

  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, cost is the cost to transmitting data through the link
   * <p/>
   * NOTE: this command should not trigger link database synchronization
   */
  public void processAttach(
          String processIP,
          short processPort,
          String simulatedIP,
          short weight
  ) {
    if (processIP.equals(rd.processIPAddress) && processPort == rd.processPortNumber) {
      console.log("Can't attach to yourself;");
      return;
    }

    if (!portsTable.hasFreePort()) {
      console.log("Can't attach any more ports;");
      return;
    }

    LinkChannel linkChannel;
    boolean accepted;
    try {
      linkChannel = new LinkChannel(new Socket(processIP, processPort));
    } catch (IOException e) {
      console.log("Could not create socket to send attach request");
      e.printStackTrace();
      return;
    }

    accepted = askAttach(
            linkChannel,
            simulatedIP,
            weight
    );

    if (!accepted) {
      console.log("Your attach request has been rejected;");
      linkChannel.close();
      return;
    }

    console.log("Your attach request has been accepted;");

    Link link = new Link(this.rd, new RouterDescription(processIP, processPort, simulatedIP), weight, linkChannel);
    this.portsTable.addLink(link);
    link.listen(this::linkPacketHandler, this.errorHandler);
  }


  /**
   * broadcast Hello to neighbors
   */
  public void processStart() {
    // HELLO all links
    portsTable.getAllLinks()
            .stream()
            .filter(link -> link.otherRouter.status == null)
            .forEach(link -> {
              LinkChannel ch = link.channel;
              try {
                link.helloInitiatedByMe = true;
                link.otherRouter.status = RouterStatus.INIT;
                ch.send(SOSPFMessageFactory.createHello(this.rd, link.otherRouter.simulatedIPAddress));
              } catch (IOException e) {
                e.printStackTrace();
              }
            });

    // Sync immediately for already established neighbors; newly established ones
    // will trigger sync when they transition to TWO_WAY in HELLO handling.
    synchronizeAndBroadcastLsd();
  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, cost is the cost to transmitting data through the link
   * <p/>
   * This command does trigger the link database synchronization
   */
  public void processConnect(
          String processIP, short processPort,
          String simulatedIP, short weight
  ) {

  }

  /**
   * output the neighbors of the routers
   */
  public void processNeighbors() {
    List<Link> neighbors = portsTable.getTwoWays();
    for (Link link : neighbors) {
      console.log(link.otherRouter.simulatedIPAddress);
    }
  }

  /**
   * disconnect with all neighbors and quit the program
   */
  public void processQuit() {

  }

  /**
   * update the cost of an attached link
   */
  public void updateWeight(
          String processIP, short processPort,
          String simulatedIP, short weight
  ) {

  }

  /**
   * update the cost of a specific port.
   * This change should trigger synchronization of the Link State Database by sending
   * a Link State Advertisement (LSA) update to all neighboring routers in the topology.
   *
   * @param portNumber the port number (0-3) to update
   * @param newWeight  the new cost/cost for the link attached to this port
   */
  public void processUpdate(short portNumber, short newWeight) {

  }

  /**
   * send an application-level message from this router to the destination router.
   * The message must be forwarded hop-by-hop according to the current shortest path.
   * <p/>
   * When you run send, the window of the router where you run the command should print:
   * "Sending message to <Destination IP>"
   * <p/>
   * For each intermediate router on the shortest path (excluding the source and destination),
   * the router window should print:
   * "Forwarding packet from <Source IP> to <Destination IP>"
   * <p/>
   * When the destination router receives the message, the router window should print:
   * "Received message from <Source IP>:"
   * "<Message>"
   *
   * @param destinationIP the simulated IP address of the destination router
   * @param message       the message content to send
   */
  public void processSend(String destinationIP, String message) throws IOException {
    if (destinationIP.equals(rd.simulatedIPAddress)) {
      console.log("Cannot send to yourself.");
      return;
    }

    List<String> path = lsd.getShortestPath(destinationIP);
    if (path == null) {
      console.log("Path not found");
      return;
    }

    path.removeFirst();

    String nextHop = path.getFirst();
    SOSPFPacket msg = SOSPFMessageFactory.createMessagee(rd, destinationIP, message);

    Optional<Link> nextHopOpt = this.portsTable.get(nextHop);
    if (nextHopOpt.isEmpty()) {
      console.log("Path not found");
      return;
    }

    nextHopOpt.get().channel.send(msg);
  }

  /**
   * handle incoming application message packet.
   * This method should be called when a router receives a SOSPFPacket with sospfType = 2 (Application Message).
   * <p/>
   * If this router is the destination (packet.dstIP equals this router's simulatedIPAddress):
   * - Print "Received message from <Source IP>:"
   * - Print the message content
   * <p/>
   * If this router is an intermediate router:
   * - Print "Forwarding packet from <Source IP> to <Destination IP>"
   * - Forward the packet to the next hop on the shortest path to the destination
   * - Do NOT print or inspect the message payload
   *
   * @param packet the received application message packet
   */
  private void handleApplicationMessage(SOSPFPacket packet) {
    if (Objects.equals(packet.dstIP, rd.simulatedIPAddress)) {
      console.log("Received message from " + packet.srcIP + ";");
      console.log("Message: " + packet.message);
      return;
    }

    List<String> path = lsd.getShortestPath(packet.dstIP);
    if (path == null || path.size() < 2) {
      console.log("Path not found");
      return;
    }

    String nextHop = path.get(1);
    Optional<Link> nextHopOpt = this.portsTable.get(nextHop);
    if (nextHopOpt.isEmpty()) {
      console.log("Path not found");
      return;
    }

    console.log("Forwarding packet from " + packet.srcIP + " to " + packet.dstIP);
    try {
      nextHopOpt.get().channel.send(packet);
    } catch (IOException e) {
      console.log("Failed to forward packet to " + nextHop);
    }
  }

  /**
   * process request from the remote router.
   * For example: when router2 tries to attach router1. Router1 can decide whether it will accept this request.
   * The intuition is that if router2 is an unknown/anomaly router, it is always safe to reject the attached request from router2.
   * <br>
   * This is only reserved for communicaton with unknown routers
   * Known routers must communicate in their dedicated channels using {@link PacketHandler}
   */
  private void requestHandler(Socket socket) {
    try {
      LinkChannel ch = new LinkChannel(socket);
      SOSPFPacket packet = ch.receive();

      boolean isAttachRequest =
              packet.sospfType == SOSPFPacket.SOSPFType.HELLO && !portsTable.containsIP(packet.srcIP);

      // This is under assumption that if a router is currently busy answering a y/n question, auto reject other attach requests
      if (isAttachRequest && console.hasPrompt()) {
        ch.send(SOSPFMessageFactory.createAttachResponse(this.rd, packet.srcIP, false));
        ch.close();
        return;
      } else if (isAttachRequest) {
        SOSPFPacket res = handleAttachRequest(packet, ch);
        ch.send(res);
        if (!res.accepted) {
          ch.close();
        }
      }
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }
  }

  private boolean askAttach(
          LinkChannel linkChannel,
          String simulatedIP,
          short weight
  ) {
    SOSPFPacket connectReqMessage = SOSPFMessageFactory.createHello(rd, simulatedIP, String.valueOf(weight));

    try {
      linkChannel.send(connectReqMessage);
      SOSPFPacket res = linkChannel.receive();

      return res != null && Boolean.TRUE.equals(res.accepted);

    } catch (IOException | ClassNotFoundException e) {
      return false;
    }
  }


  private SOSPFPacket handleAttachRequest(SOSPFPacket packet, LinkChannel linkChannel) {
    console.log("\nReceived " + packet.displayString() + " from " + packet.srcIP);

    String answer;
    answer = console
            .requestPromptAsync("Do you accept this request? (Y/N)")
            .join();  // block and wait for response

    boolean accepted = answer.equalsIgnoreCase("y") && portsTable.hasFreePort();

    if (accepted) {
      console.log("You accepted the attach request;");

      Link link = new Link(
              this.rd,
              new RouterDescription(packet.srcProcessIP, packet.srcProcessPort, packet.srcIP),
              Integer.parseInt(packet.message),
              linkChannel
      );
      this.portsTable.addLink(link);
      link.listen(this::linkPacketHandler, this.errorHandler);

    } else {
      if (!portsTable.hasFreePort()) {
        console.log("No free ports, will reject the attach request;");
      }
      console.log("You rejected the attach request");
    }

    return SOSPFMessageFactory.createAttachResponse(rd, packet.srcIP, accepted);
  }

  private void linkPacketHandler(SOSPFPacket packet, LinkChannel ch) {
    if (packet == null) {
      console.log("Received null packet");
      return;
    }

    console.log("Received " + packet.displayString() + " FROM " + packet.srcIP + ";");

    if (isAttachRequestResponse(packet) && handleAttachRequestResponse(packet)) {
      return;
    }

    switch (packet.sospfType) {
      case APPLICATION_MSG -> handleApplicationMessage(packet);
      case HELLO -> handleDatabaseSyncHello(packet, ch);
      case LINKSTATE_UPDATE -> handleLinkStateUpdate(packet);
      default -> {
      }
    }
  }

  private boolean isAttachRequestResponse(SOSPFPacket packet) {
    return packet.sospfType == SOSPFPacket.SOSPFType.HELLO && packet.accepted != null;
  }

  private boolean handleAttachRequestResponse(SOSPFPacket packet) {
    if (packet.accepted) {
      console.log("Attach accepted by " + packet.srcIP + ";");
    } else {
      console.log("Attach rejected by " + packet.srcIP + ";");
    }
    return true;
  }

  private void handleDatabaseSyncHello(SOSPFPacket packet, LinkChannel ch) {
    String otherRouterIP = packet.srcIP;
    Optional<Link> linkOpt = portsTable.get(otherRouterIP);

    if (linkOpt.isEmpty()) {
      // Not a neighbour, should not really end up in this state, for now just ignore the packet
      return;
    }

    Link link = linkOpt.get();
    RouterStatus status = link.otherRouter.status;
    boolean shouldReplyHello = false;
    boolean becameTwoWay = false;

    if (status == null) {
      link.otherRouter.status = RouterStatus.INIT;
      shouldReplyHello = true;
    } else if (status == RouterStatus.INIT) {
      link.otherRouter.status = RouterStatus.TWO_WAY;
      becameTwoWay = true;
      if (link.helloInitiatedByMe) {
        shouldReplyHello = true;
        link.helloInitiatedByMe = false;
      }
    } else if (status == RouterStatus.TWO_WAY) {
      return;
    }

    console.log("set " + otherRouterIP + " STATE to " + link.otherRouter.status + ";");
    if (shouldReplyHello) {
      try {
        ch.send(SOSPFMessageFactory.createHello(this.rd, otherRouterIP));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    if (becameTwoWay) {
      synchronizeAndBroadcastLsd();
    }
  }

  private void handleLinkStateUpdate(SOSPFPacket packet) {
    if (packet.lsaArray == null || packet.lsaArray.isEmpty()) {
      return;
    }

    List<LSA> newlyAcceptedLSA = new ArrayList<>();

    // Update existing LSAs
    packet.lsaArray.stream()
            .filter(lsa -> !Objects.equals(lsa.linkStateID, rd.simulatedIPAddress)) // Do not update my own LSA
            .forEach(lsa -> {
              Optional<LSA> existingLSA = this.lsd.getLSA(lsa.linkStateID);
              if (existingLSA.isEmpty() || lsa.getSeqNumber() > existingLSA.get().getSeqNumber()) {
                LSA storedCopy = LSA.copyOf(lsa);
                this.lsd.addLSA(storedCopy.linkStateID, storedCopy);
                newlyAcceptedLSA.add(LSA.copyOf(storedCopy));
              }
            });

    // Propagate new LSAs for neighbous
    if (newlyAcceptedLSA.isEmpty()) {
      return;
    }

    this.portsTable.getTwoWays()
            .stream()
            .filter(link -> !Objects.equals(link.otherRouter.simulatedIPAddress, packet.srcIP))
            .forEach(link -> {
              SOSPFPacket msg = SOSPFMessageFactory.createLSAUPDATE(
                      rd,
                      link.otherRouter.simulatedIPAddress,
                      newlyAcceptedLSA
              );

              LinkChannel neighbour_ch = link.channel;
              try {
                neighbour_ch.send(msg);
              } catch (IOException e) {
                console.log("Could not send LSAUpdate to " + link.otherRouter.simulatedIPAddress);
              }
            });
  }

  /**
   * It can happen that our LSA and actually connected links are out of things like disconnection.
   * Update this router's LSA states to the current links states.
   */
  private synchronized void synchronizeAndBroadcastLsd() {
    LSA myLSA = lsd.getMyLSA();

    List<LinkDescription> newLinks = this.portsTable
            .getTwoWays()
            .stream()
            .map(LinkDescription::fromLink)
            .toList();

    List<LinkDescription> currentNonSelfLinks = myLSA.getLinks()
            .stream()
            .filter(ld -> !(myLSA.linkStateID.equals(ld.linkID) && ld.weight == 0))
            .toList();

    if (!newLinks.equals(currentNonSelfLinks)) {
      myLSA.clearLinks();
      myLSA.addLinks(newLinks);
      myLSA.bumpSeqNumber();
    }

    List<LSA> snapshot = lsd.getSnapshot();
    portsTable.getTwoWays().forEach(link -> {
      SOSPFPacket msg = SOSPFMessageFactory.createLSAUPDATE(
              rd,
              link.otherRouter.simulatedIPAddress,
              snapshot
      );

      try {
        link.channel.send(msg);
      } catch (IOException e) {
        console.log("Failed to send LSAUpdate to " + link.otherRouter.simulatedIPAddress + ": " + e.getMessage());
      }
    });
  }


  /**
   * Helper command for debugging. Shows the state of the ports table.
   */
  public void processPort() {
    console.log(this.portsTable.toString());
  }

  /**
   * Helper command for debugging. Shows the state of the LSD.
   */
  public void processLsd() {
    console.log(this.lsd.toString());
  }
}
