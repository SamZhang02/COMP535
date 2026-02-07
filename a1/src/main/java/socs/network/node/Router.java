package socs.network.node;

import socs.network.cli.Console;
import socs.network.message.SOSPFMessageFactory;
import socs.network.message.SOSPFPacket;
import socs.network.node.ports.PortsTable;
import socs.network.transport.LinkChannel;
import socs.network.transport.RouterTransport;
import socs.network.util.Configuration;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class Router {
  RouterDescription rd = new RouterDescription();
  protected LinkStateDatabase lsd;

  private Console console;
  private final RouterTransport routerTransport;
  private final PortsTable portsTable;


  public Router(Configuration config, RouterTransport rt, Console console, PortsTable portsTable) {
    lsd = new LinkStateDatabase(rd);

    rd.processIPAddress = config.getString("socs.network.router.pip");
    rd.processPortNumber = config.getShort("socs.network.router.port");
    rd.simulatedIPAddress = config.getString("socs.network.router.ip");

    routerTransport = rt;
    this.console = console;
    this.portsTable = portsTable;
  }

  public void terminal() {
    this.rd.print();

    Thread clientServiceThread = this.routerTransport.serve(this::requestHandler);
    Thread consoleThread = this.startConsoleThread();

    try {
      while (true) {
        console.print(">> ");
        String command = console.getCommandQueue().take();
        if (!handleCommand(command)) break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      clientServiceThread.interrupt();
      console.stop();
    }
  }


  /**
   * output the shortest path to the given destination ip
   * <p/>
   * format: source ip address  -> ip address -> ... -> destination ip
   *
   * @param destinationIP the ip adderss of the destination simulated router
   */
  private void processDetect(String destinationIP) {

  }

  /**
   * disconnect with the router identified by the given destination ip address
   * Notice: this command should trigger the synchronization of database
   *
   * @param portNumber the port number which the link attaches at
   */
  private void processDisconnect(short portNumber) {

  }

  private boolean askAttach(
          LinkChannel linkChannel,
          String processIP,
          short processPort,
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

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * NOTE: this command should not trigger link database synchronization
   */
  private void processAttach(
          String processIP,
          short processPort,
          String simulatedIP,
          short weight
  ) {
    if (!portsTable.hasFreePort()) {
      console.println("Can't attach any more ports;");
      return;
    }

    LinkChannel linkChannel;
    boolean accepted;
    try {
      linkChannel = new LinkChannel(new Socket(processIP, processPort));
    } catch (IOException e) {
      console.println("Could not create socket to send attach request");
      e.printStackTrace();
      return;
    }

    accepted = askAttach(
            linkChannel,
            processIP,
            processPort,
            simulatedIP,
            weight
    );

    if (!accepted) {
      console.println("Your attach request has been rejected;");
      linkChannel.close();
      return;
    }

    console.println("Your attach request has been accepted;");

    Link link = new Link(this.rd, new RouterDescription(processIP, processPort, simulatedIP), weight, linkChannel);
    this.portsTable.addLink(link);
    link.listen(this::handleLinkPacket);
  }


  /**
   * broadcast Hello to neighbors
   */
  private void processStart() {
    // for each link in port
    // send HELLO
    // The other link should get the hello and set themselves to INIT
    // we should expect a HELLO back
    // once we get a HELLO back, set our status as TWO WAYS
    // send another HELLO to let the other router know its 2 ways
  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * This command does trigger the link database synchronization
   */
  private void processConnect(
          String processIP, short processPort,
          String simulatedIP, short weight
  ) {

  }

  /**
   * output the neighbors of the routers
   */
  private void processNeighbors() {
    List<Link> neighbors = portsTable.getTwoWays();
    for (Link link : neighbors) {
      console.println(link.otherRouter.simulatedIPAddress);
    }
  }

  /**
   * disconnect with all neighbors and quit the program
   */
  private void processQuit() {

  }

  /**
   * update the weight of an attached link
   */
  private void updateWeight(
          String processIP, short processPort,
          String simulatedIP, short weight
  ) {

  }

  /**
   * update the weight of a specific port.
   * This change should trigger synchronization of the Link State Database by sending
   * a Link State Advertisement (LSA) update to all neighboring routers in the topology.
   *
   * @param portNumber the port number (0-3) to update
   * @param newWeight  the new weight/cost for the link attached to this port
   */
  private void processUpdate(short portNumber, short newWeight) {

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
  private void processSend(String destinationIP, String message) {

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

  }


  private Thread startConsoleThread() {
    console = new Console();
    Thread consoleThread = new Thread(console, "console-thread");
    consoleThread.setDaemon(true);

    consoleThread.start();
    return consoleThread;
  }

  /**
   * process request from the remote router.
   * For example: when router2 tries to attach router1. Router1 can decide whether it will accept this request.
   * The intuition is that if router2 is an unknown/anomaly router, it is always safe to reject the attached request from router2.
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

      console.print(">> ");
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }
  }

  private SOSPFPacket handleAttachRequest(SOSPFPacket packet, LinkChannel linkChannel) {
    console.println("\nReceived " + packet.displayString() + " from " + packet.srcIP);

    String answer;
    answer = console
            .requestPromptAsync("Do you accept this request? (Y/N)")
            .join();  // block and wait for response

    boolean accepted = answer.equalsIgnoreCase("y") && portsTable.hasFreePort();

    if (accepted) {
      console.println("You accepted the attach request;");

      Link link = new Link(
              this.rd,
              new RouterDescription(packet.srcProcessIP, packet.srcProcessPort, packet.srcIP),
              Integer.parseInt(packet.message),
              linkChannel
      );
      this.portsTable.addLink(link);
      link.listen(this::handleLinkPacket);

    } else {
      if (!portsTable.hasFreePort()) {
        console.println("No free ports, will reject the attach request;");
      }
      console.println("You rejected the attach request");
    }

    return SOSPFMessageFactory.createAttachResponse(rd, packet.srcIP, accepted);
  }

  private void handleLinkPacket(SOSPFPacket packet) {
    if (packet == null) {
      return;
    }

    if (packet.sospfType == SOSPFPacket.SOSPFType.HELLO && packet.accepted != null) {
      if (Boolean.TRUE.equals(packet.accepted)) {
        console.println("Attach accepted by " + packet.srcIP + ";");
      } else {
        console.println("Attach rejected by " + packet.srcIP + ";");
      }
    }
  }

  private boolean handleCommand(String command) {
    if (command.startsWith("detect ")) {
      String[] cmdLine = command.split(" ");
      processDetect(cmdLine[1]);
    } else if (command.startsWith("disconnect ")) {
      String[] cmdLine = command.split(" ");
      processDisconnect(Short.parseShort(cmdLine[1]));
    } else if (command.startsWith("quit")) {
      processQuit();
    } else if (command.startsWith("attach ")) {
      String[] cmdLine = command.split(" ");
      processAttach(
              cmdLine[1], Short.parseShort(cmdLine[2]),
              cmdLine[3], Short.parseShort(cmdLine[4])
      );
    } else if (command.equals("start")) {
      processStart();
    } else if (command.equals("connect ")) {
      String[] cmdLine = command.split(" ");
      processConnect(
              cmdLine[1], Short.parseShort(cmdLine[2]),
              cmdLine[3], Short.parseShort(cmdLine[4])
      );
    } else if (command.equals("neighbors")) {
      //output neighbors
      processNeighbors();
    } else if (command.startsWith("send ")) {
      //send [Destination IP] [Message]
      String[] cmdLine = command.split(" ", 3);
      if (cmdLine.length >= 3) {
        processSend(cmdLine[1], cmdLine[2]);
      } else {
        console.println("Usage: send [Destination IP] [Message]");
      }
    } else if (command.startsWith("update ")) {
      //update [port_number] [new_weight]
      String[] cmdLine = command.split(" ");
      if (cmdLine.length >= 3) {
        processUpdate(Short.parseShort(cmdLine[1]), Short.parseShort(cmdLine[2]));
      } else {
        console.println("Usage: update [port_number] [new_weight]");
      }
    } else if (command.startsWith("port")) {
      // For debugging
      console.println(this.portsTable.toString());
      ;
    } else {
      //invalid command
      return false;
    }
    return true;
  }
}
