package socs.network.node.ports;

import socs.network.node.Link;

import java.util.Arrays;

public class PortsTable {
  private final int capacity;
  private final Link[] ports;

  public PortsTable(int capacity) {
    this.capacity = capacity;
    this.ports = new Link[capacity];
  }

  public int capacity() {
    return this.capacity;
  }

  public int available() {
    int res = 0;
    for (int i = 0; i < this.capacity; i++) {
      if (ports[i] == null) {
        res += 1;
      }
    }

    return res;
  }

  public int used() {
    return this.capacity() - this.available();

  }

  public boolean hasFreePort() {
    return this.available() > 0;
  }

  public void addLink(Link link) throws PortsTableException{
    int freeIdx = getFreePortIdx();
    if (freeIdx == -1) {
      throw new PortsTableException("No free ports available");
    }

    this.ports[freeIdx] = link;
  }

  public Link get(int port) {
    return this.ports[port];
  }

  public void removeLink(Link link) {
    for (int i = 0; i < this.capacity; i++) {
      if (this.ports[i].equals(link)) {
        this.ports[i] = null;
      }
    }
  }

  public void removeLinkAt(int port) {
    this.ports[port] = null;
  }

  public boolean containsIP(String simulatedIP) {
    for (int i = 0; i < this.capacity; i++) {
      Link port = this.ports[i];
      if (port != null &&
              port.router2 != null &&
              simulatedIP.equals(port.router2.simulatedIPAddress)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    Link[] snapshot = new Link[this.capacity];
    for (int i = 0; i < this.capacity; i++) {
      snapshot[i] = this.get(i);
    }
    return Arrays.toString(snapshot);
  }


  private int getFreePortIdx() {
    for (int i = 0; i < this.capacity; i++) {
      if (ports[i] == null) {
        return i;
      }
    }

    return -1;
  }
}
