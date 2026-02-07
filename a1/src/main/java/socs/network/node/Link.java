package socs.network.node;

public class Link {

  public RouterDescription ourRouter; // This should be OUR router
  public RouterDescription otherRouter; // This is the OTHER router
  public int weight;

  public Link(RouterDescription ourRouter, RouterDescription otherRouter, int weight) {
    this.ourRouter = ourRouter;
    this.otherRouter = otherRouter;
    this.weight = weight;
  }

  @Override
  public String toString() {
    return "Link{" +
            "ourRouter=" + ourRouter +
            ", otherRouter=" + otherRouter +
            ", weight=" + weight +
            '}';
  }
}
