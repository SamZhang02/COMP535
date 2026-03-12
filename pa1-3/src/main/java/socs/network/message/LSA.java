package socs.network.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// Link State Advertisement
public class LSA implements Serializable {

  /**
   * IP address of the router originate this LSA
   */
  public String linkStateID;

  /**
   * Version number
   */
  private int lsaSeqNumber = Integer.MIN_VALUE;

  /**
   * Description of Neighbours
   */
  private List<LinkDescription> links = new ArrayList<>();

  public int getSeqNumber() {
    return this.lsaSeqNumber;
  }

  public void setSeqNumber(int seqNumber) {
    this.lsaSeqNumber = seqNumber;
  }

  public int bumpSeqNumber() {
    lsaSeqNumber++;
    return lsaSeqNumber;
  }

  public void clearLinks() {
    links.removeIf(ld -> !(linkStateID != null
            && linkStateID.equals(ld.linkID)
            && ld.weight == 0));
  }

  public void addLinks(List<LinkDescription> newLinks) {
    links.addAll(newLinks);
  }

  public boolean addLink(LinkDescription ld) {
    return links.add(ld);
  }


  public Optional<LinkDescription> getLink(String id) {
    return links.stream().filter(l -> Objects.equals(l.linkID, id)).findFirst();
  }


  public void removeLink(String ip) {
    this.links.removeIf(ld -> Objects.equals(ld.linkID, ip));
  }

  public List<LinkDescription> getLinks() {
    return this.links;
  }

  public static LSA copyOf(LSA source) {
    LSA copy = new LSA();
    copy.linkStateID = source.linkStateID;
    copy.setSeqNumber(source.getSeqNumber());
    copy.clearLinks();
    copy.addLinks(source.getLinks().stream().map(LinkDescription::copyOf).toList());
    return copy;
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
