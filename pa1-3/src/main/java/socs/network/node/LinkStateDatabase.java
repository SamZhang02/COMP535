package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

import java.util.*;

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
   * This method uses Dijkstra's algorithm with link weights (not hop count) to find the shortest path.
   * The weights are stored in the cost field of LinkDescription objects in the LSA entries.
   * <p/>
   * format: source ip address -> ip address -> ... -> destination ip
   *
   * @param destinationIP the simulated IP address of the destination router
   * @return the shortest path as a list of string (ip address), or null if no path exists
   */
  List<String> getShortestPath(String destinationIP) {
    Map<String, Integer> costTable = new HashMap<>();
    Map<String, String> prevTable = new HashMap<>();
    String sourceIP = rd.simulatedIPAddress;

    this._store.forEach((ip, _) -> {
              costTable.put(ip, Integer.MAX_VALUE);
              prevTable.put(ip, null);
            }
    );

    PriorityQueue<State> pq = new PriorityQueue<>(Comparator.comparingInt(State::cost));
    costTable.put(sourceIP, 0);
    pq.add(new State(sourceIP, 0));

    while (!pq.isEmpty()) {
      State curr = pq.poll();
      if (curr.cost() > costTable.getOrDefault(curr.ip(), Integer.MAX_VALUE)) {
        continue;
      }

      LSA currLSA = this._store.get(curr.ip());
      if (currLSA == null) {
        continue;
      }

      List<LinkDescription> nexts = currLSA.getLinks();
      for (LinkDescription next : nexts) {
        if (next.linkID == null) {
          continue;
        }
        costTable.putIfAbsent(next.linkID, Integer.MAX_VALUE);
        prevTable.putIfAbsent(next.linkID, null);

        if (curr.cost() == Integer.MAX_VALUE) {
          continue;
        }

        int newCost = curr.cost() + next.weight;
        if (newCost < costTable.get(next.linkID)) {
          costTable.put(next.linkID, newCost);
          prevTable.put(next.linkID, curr.ip());

          State state = new State(next.linkID, newCost);
          pq.add(state);
        }
      }
    }

    if (!costTable.containsKey(destinationIP) || costTable.get(destinationIP) == Integer.MAX_VALUE) {
      return null;
    }

    LinkedList<String> path = new LinkedList<>();
    String cursor = destinationIP;
    while (cursor != null) {
      path.addFirst(cursor);
      cursor = prevTable.get(cursor);
    }

    if (path.isEmpty() || !sourceIP.equals(path.getFirst())) {
      return null;
    }

    return path;
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

  synchronized public List<LSA> getImmutableSnapshot() {
    return this._store.values()
            .stream()
            .map(LSA::copyOf)
            .toList();
  }

  //initialize the linkstate database by adding an entry about the router itself
  private LSA initLinkStateDatabase() {
    LSA lsa = new LSA();
    lsa.linkStateID = rd.simulatedIPAddress;

    // ld w myself
    LinkDescription ld = new LinkDescription();
    ld.linkID = rd.simulatedIPAddress;
    ld.tosMetrics = 0;
    ld.weight = 0; //self-link has cost 0
    lsa.addLink(ld);

    return lsa;
  }


  synchronized public String toString() {
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

record State(String ip, int cost) {
}
