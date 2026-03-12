package socs.network.node.ports;

import org.junit.Ignore;
import org.junit.Test;
import socs.network.node.Link;
import socs.network.node.RouterDescription;
import socs.network.transport.LinkChannel;

import java.io.IOException;
import java.net.Socket;

import static org.junit.Assert.assertEquals;


public class PortsTableTest {

  @Test
  public void capacity_shouldReturnCapacity() {
    PortsTable table = new PortsTable(10);

    assertEquals(10, table.capacity());
  }

  @Ignore
  @Test
  public void addLink_shouldAddLinkIfAvailable() throws IOException {
    PortsTable table = new PortsTable(10);
    RouterDescription rd1 = new RouterDescription();
    RouterDescription rd2 = new RouterDescription();
    LinkChannel ch = new LinkChannel(new Socket());
    Link link = new Link(rd1, rd2, 2, ch);

    table.addLink(link);

    assertEquals(9, table.available());
    assertEquals(1, table.used());
  }

}
