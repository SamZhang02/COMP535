package socs.network.message;

import socs.network.node.Link;

import java.io.Serializable;
import java.util.UUID;

public class LinkDescription implements Serializable {
  public String linkID;
  public int tosMetrics;
  public int weight; //link weight/cost for shortest path calculation


  static public LinkDescription fromLink(Link link) {
    LinkDescription ld = new LinkDescription();
    ld.linkID = UUID.randomUUID().toString();
    ld.tosMetrics = 0; // Did not receive instruction, for now putting 0
    ld.weight = link.weight;

    return ld;
  }
}
