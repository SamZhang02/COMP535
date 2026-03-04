package socs.network.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// Link State Advertisement
public class LSA implements Serializable {

  //IP address of the router originate this LSA
  public String linkStateID;
  private int lsaSeqNumber = Integer.MIN_VALUE;

  private List<LinkDescription> links = new ArrayList<>();

  public int getSeqNumber() {
    return this.lsaSeqNumber;
  }

  public int bumpSeqNumber() {
    lsaSeqNumber++;
    return lsaSeqNumber;
  }

  public void setLinks(List<LinkDescription> newLinks) {
    links = newLinks;
  }

  public boolean addLink(LinkDescription ld) {
    return links.add(ld);
  }

  public List<LinkDescription> getLinks() {
    return this.links;
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(linkStateID + ":").append(lsaSeqNumber + "\n");
    for (LinkDescription ld : links) {
      sb.append(ld);
    }
    sb.append("\n");
    return sb.toString();
  }
}
