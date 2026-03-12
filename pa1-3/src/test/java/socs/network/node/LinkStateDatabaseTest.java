package socs.network.node;

import org.junit.Test;
import socs.network.message.LSA;
import socs.network.message.LinkDescription;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LinkStateDatabaseTest {

  @Test
  public void getShortestPath_shouldReturnFullPathWithLowestTotalWeight() {
    RouterDescription rd = new RouterDescription("127.0.0.1", (short) 2000, "A");
    LinkStateDatabase lsd = new LinkStateDatabase(rd);

    LSA a = lsd.getMyLSA();
    a.addLink(link("B", 1));
    a.addLink(link("C", 5));

    lsd.addLSA("B", lsa("B", link("A", 1), link("C", 1), link("D", 4)));
    lsd.addLSA("C", lsa("C", link("A", 5), link("B", 1), link("D", 1)));
    lsd.addLSA("D", lsa("D", link("B", 4), link("C", 1)));

    List<String> path = lsd.getShortestPath("D");
    assertEquals(Arrays.asList("A", "B", "C", "D"), path);
  }

  @Test
  public void getShortestPath_shouldReturnNullIfDestinationUnreachable() {
    RouterDescription rd = new RouterDescription("127.0.0.1", (short) 2001, "A");
    LinkStateDatabase lsd = new LinkStateDatabase(rd);

    LSA a = lsd.getMyLSA();
    a.addLink(link("B", 1));
    lsd.addLSA("B", lsa("B", link("A", 1)));

    assertNull(lsd.getShortestPath("Z"));
  }

  @Test
  public void getShortestPath_shouldHandleLongPathAndIsolatedComponent() {
    RouterDescription rd = new RouterDescription("127.0.0.1", (short) 2003, "A");
    LinkStateDatabase lsd = new LinkStateDatabase(rd);

    // Main connected component from A:
    // A -> B -> C -> D -> E -> F -> G
    // with a few heavier shortcuts to ensure the long path is actually chosen.
    LSA a = lsd.getMyLSA();
    a.addLink(link("B", 2));
    a.addLink(link("D", 20));

    lsd.addLSA("B", lsa("B", link("A", 2), link("C", 2), link("E", 15)));
    lsd.addLSA("C", lsa("C", link("B", 2), link("D", 2), link("F", 20)));
    lsd.addLSA("D", lsa("D", link("C", 2), link("E", 2), link("A", 20)));
    lsd.addLSA("E", lsa("E", link("D", 2), link("F", 2), link("B", 15)));
    lsd.addLSA("F", lsa("F", link("E", 2), link("G", 2), link("C", 20)));
    lsd.addLSA("G", lsa("G", link("F", 2)));

    // Isolated component H <-> I (disconnected from A's component)
    lsd.addLSA("H", lsa("H", link("I", 1)));
    lsd.addLSA("I", lsa("I", link("H", 1)));

    assertEquals(List.of("A", "B", "C", "D", "E", "F", "G"), lsd.getShortestPath("G"));
    assertNull(lsd.getShortestPath("H"));
  }

  @Test
  public void getShortestPath_shouldReturnSourceWhenDestinationIsSource() {
    RouterDescription rd = new RouterDescription("127.0.0.1", (short) 2002, "A");
    LinkStateDatabase lsd = new LinkStateDatabase(rd);

    assertEquals(List.of("A"), lsd.getShortestPath("A"));
  }

  @Test
  public void getShortestPath_shouldPreferR3ChainOverR7Detour() {
    RouterDescription rd = new RouterDescription("127.0.0.1", (short) 2010, "R1");
    LinkStateDatabase lsd = new LinkStateDatabase(rd);

    LSA r1 = lsd.getMyLSA();
    r1.addLink(link("R2", 2));

    lsd.addLSA("R2", lsa("R2", link("R1", 2), link("R3", 2), link("R7", 5)));
    lsd.addLSA("R3", lsa("R3", link("R2", 2), link("R4", 2), link("R7", 5)));
    lsd.addLSA("R4", lsa("R4", link("R3", 2), link("R5", 2)));
    lsd.addLSA("R5", lsa("R5", link("R4", 2), link("R7", 10)));
    lsd.addLSA("R7", lsa("R7", link("R2", 5), link("R3", 5), link("R5", 10)));

    assertEquals(List.of("R1", "R2", "R3", "R4", "R5"), lsd.getShortestPath("R5"));
  }

  private static LSA lsa(String origin, LinkDescription... links) {
    LSA lsa = new LSA();
    lsa.linkStateID = origin;
    lsa.addLinks(List.of(links));
    return lsa;
  }

  private static LinkDescription link(String to, int weight) {
    LinkDescription ld = new LinkDescription();
    ld.linkID = to;
    ld.tosMetrics = 0;
    ld.weight = weight;
    return ld;
  }
}
