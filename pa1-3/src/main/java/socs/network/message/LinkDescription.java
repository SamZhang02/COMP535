package socs.network.message;

import socs.network.node.Link;

import java.io.Serializable;
import java.util.Objects;

public class LinkDescription implements Serializable {
  public String linkID;
  public int tosMetrics;
  public int weight; //link weight/cost for shortest path calculation


  static public LinkDescription fromLink(Link link) {
    LinkDescription ld = new LinkDescription();
    ld.linkID = link.otherRouter.simulatedIPAddress;
    ld.tosMetrics = 0; // Did not receive instruction, for now putting 0
    ld.weight = link.weight;

    return ld;
  }

  public static LinkDescription copyOf(LinkDescription source) {
    LinkDescription copy = new LinkDescription();
    copy.linkID = source.linkID;
    copy.tosMetrics = source.tosMetrics;
    copy.weight = source.weight;
    return copy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LinkDescription that)) {
      return false;
    }
    return tosMetrics == that.tosMetrics
            && weight == that.weight
            && Objects.equals(linkID, that.linkID);
  }

  @Override
  public int hashCode() {
    return Objects.hash(linkID, tosMetrics, weight);
  }
}
