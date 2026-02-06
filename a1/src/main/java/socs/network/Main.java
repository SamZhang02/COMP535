package socs.network;

import socs.network.cli.Console;
import socs.network.node.Router;
import socs.network.transport.RouterTransport;
import socs.network.util.Configuration;

import java.io.IOException;

public class Main {

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.out.println("usage: program conf_path");
      System.exit(1);
    }

    Configuration config = new Configuration(args[0]);
    Console console = new Console();
    RouterTransport routerTransport = new RouterTransport(config);

    Router r = new Router(config, routerTransport, console);

    r.terminal();
  }
}
