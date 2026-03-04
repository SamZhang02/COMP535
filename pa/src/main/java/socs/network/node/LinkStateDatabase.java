package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LinkStateDatabase {

  //linkID => LSAInstance
  private Map<String, LSA> _store = new HashMap<String, LSA>();

  private RouterDescription rd = null;

  public LinkStateDatabase(RouterDescription routerDescription) {
    rd = routerDescription;
    LSA l = initLinkStateDatabase();
    _store.put(l.linkStateID, l);
  }

  /**
   * output the shortest path from this router to the destination with the given IP address
   * <p/>
   * This method should use Dijkstra's algorithm with link weights (not hop count) to find the shortest path.
   * The weights are stored in the weight field of LinkDescription objects in the LSA entries.
   * <p/>
   * format: source ip address -> ip address -> ... -> destination ip
   *
   * @param destinationIP the simulated IP address of the destination router
   * @return the shortest path as a string, or null if no path exists
   */
  String getShortestPath(String destinationIP) {
    //TODO: fill the implementation here
    return null;
  }

  synchronized public LSA getMyLSA() {
    return this._store.get(rd.simulatedIPAddress);
  }

  synchronized public void addLSA(String id, LSA lsa) {
    this._store.put(id, lsa);
  }

  synchronized public LSA removeLSA(String id) {
    return this._store.remove(id);
  }

  synchronized public Optional<LSA> getLSA(String id) {
    return Optional.ofNullable(this._store.get(id));
  }

  synchronized public List<LSA> getSnapshot() {
    return this._store.values().stream().toList();
  }

  //initialize the linkstate database by adding an entry about the router itself
  private LSA initLinkStateDatabase() {
    LSA lsa = new LSA();
    lsa.linkStateID = rd.simulatedIPAddress;

    // ld w myself
    LinkDescription ld = new LinkDescription();
    ld.linkID = rd.simulatedIPAddress;
    ld.tosMetrics = 0;
    ld.weight = 0; //self-link has weight 0
    lsa.addLink(ld);

    return lsa;
  }


  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (LSA lsa : _store.values()) {
      sb.append(lsa.linkStateID).append("(" + lsa.getSeqNumber() + ")").append(":\t");
      for (LinkDescription ld : lsa.getLinks()) {
        sb.append(ld.linkID).append(",").append(ld).append(",").
                append(ld.weight).append("\t");
      }
      sb.append("\n");
    }
    return sb.toString();
  }

}
