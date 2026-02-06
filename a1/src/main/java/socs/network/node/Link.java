package socs.network.node;

public class Link {

  RouterDescription router1;
  RouterDescription router2;
  int weight;

  public Link(RouterDescription r1, RouterDescription r2, int weight) {
    this.router1 = r1;
    this.router2 = r2;
    this.weight = weight;
  }

  @Override
  public String toString() {
    return "Link{" +
            "router1=" + router1 +
            ", router2=" + router2 +
            ", weight=" + weight +
            '}';
  }
}
