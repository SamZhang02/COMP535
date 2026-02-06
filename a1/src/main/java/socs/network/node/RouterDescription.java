package socs.network.node;


public class RouterDescription {
  //used to socket communication
  public String processIPAddress;
  public short processPortNumber;
  //used to identify the router in the simulated network space
  public String simulatedIPAddress;
  //status of the router
  public RouterStatus status;

  public RouterDescription() {
  }

  public RouterDescription(String processIPAddress, short processPortNumber, String simulatedIPAddress) {
    this.processIPAddress = processIPAddress;
    this.processPortNumber = processPortNumber;
    this.simulatedIPAddress = simulatedIPAddress;
  }


  public void print() {
    System.out.println("========================================");
    System.out.printf("Process IP     : %s%n", processIPAddress);
    System.out.printf("Process port   : %d%n", processPortNumber);
    System.out.printf("Simulated IP   : %s%n", simulatedIPAddress);
    System.out.println("========================================");
  }
}


