package socs.network.node;

public class RouterDescription {
  //used to socket communication
  String processIPAddress;
  short processPortNumber;
  //used to identify the router in the simulated network space
  String simulatedIPAddress;
  //status of the router
  RouterStatus status;

  public void print(){
    System.out.println("========================================");
    System.out.printf("Process IP     : %s%n", processIPAddress);
    System.out.printf("Process port   : %d%n", processPortNumber);
    System.out.printf("Simulated IP   : %s%n", simulatedIPAddress);
    System.out.println("========================================");
  }
}


