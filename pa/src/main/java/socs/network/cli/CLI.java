package socs.network.cli;

import socs.network.node.Router;

public class CLI {
  private final Router router;
  private final Console console;


  public CLI(Router router, Console console) {
    this.router = router;
    this.console = console;
  }

  public void start() {
    router.start();
    console.start();

    try {
      while (true) {
        String command = console.getCommandQueue().take();
        if (!handleCommand(command)) {
          break;
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      router.stop();
      console.stop();
    }
  }

  private boolean handleCommand(String rawCommand) {
    String command = rawCommand == null ? "" : rawCommand.trim();
    if (command.isEmpty()) {
      return true;
    }

    String[] parts = command.split("\\s+");
    String keyword = parts[0];

    try {
      switch (keyword) {
        case "detect":
          if (parts.length != 2) {
            console.log("Usage: detect [destination IP]");
            return true;
          }
          router.processDetect(parts[1]);
          return true;
        case "disconnect":
          if (parts.length != 2) {
            console.log("Usage: disconnect [port_number]");
            return true;
          }
          router.processDisconnect(Short.parseShort(parts[1]));
          return true;
        case "quit":
          if (parts.length != 1) {
            console.log("Usage: quit");
            return true;
          }
          router.processQuit();
          return false;
        case "attach":
          if (parts.length != 5) {
            console.log("Usage: attach [process IP] [process port] [simulated IP] [weight]");
            return true;
          }
          router.processAttach(
                  parts[1],
                  Short.parseShort(parts[2]),
                  parts[3],
                  Short.parseShort(parts[4])
          );
          return true;
        case "start":
          if (parts.length != 1) {
            console.log("Usage: start");
            return true;
          }
          router.processStart();
          return true;
        case "connect":
          if (parts.length != 5) {
            console.log("Usage: connect [process IP] [process port] [simulated IP] [weight]");
            return true;
          }
          router.processConnect(
                  parts[1],
                  Short.parseShort(parts[2]),
                  parts[3],
                  Short.parseShort(parts[4])
          );
          return true;
        case "neighbors":
          if (parts.length != 1) {
            console.log("Usage: neighbors");
            return true;
          }
          router.processNeighbors();
          return true;
        case "send":
          String[] sendParts = command.split("\\s+", 3);
          if (sendParts.length != 3) {
            console.log("Usage: send [Destination IP] [Message]");
            return true;
          }
          router.processSend(sendParts[1], sendParts[2]);
          return true;
        case "update":
          if (parts.length != 3) {
            console.log("Usage: update [port_number] [new_weight]");
            return true;
          }
          router.processUpdate(Short.parseShort(parts[1]), Short.parseShort(parts[2]));
          return true;
        case "port":
          if (parts.length != 1) {
            console.log("Usage: port");
            return true;
          }
          router.processPort();
          return true;
        default:
          console.log("Invalid command");
          return true;
      }
    } catch (NumberFormatException e) {
      console.log("Invalid number in command");
      return true;
    }
  }

}
