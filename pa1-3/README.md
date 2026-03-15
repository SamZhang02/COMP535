# COMP535 Project Starter Code

## Requirements

To compile and run this project, you need:

- **Java**: Version 22 or higher (JDK 22+)
- **Maven**: Version 3.0 or higher

### Checking Your Versions

You can check if you have the required versions installed:

```bash
java -version
mvn -version
```

If you don't have these installed, please install them before proceeding.

## Building the Project

To compile the project:

```bash
mvn clean compile
```

To compile and create a JAR file:

```bash
mvn clean package
```

The compiled classes will be in `target/classes/` directory.

## Running the Program

Create an executable JAR with dependencies and run it:

```bash
mvn clean package assembly:single
java -jar target/COMP535-1.0-SNAPSHOT-jar-with-dependencies.jar conf/router1.conf
```

## Scripts

There is a justfile with scripts for building, running and testing the program.

```just
just build
```

```just
just test
```

Running conf/router1.conf:

```just
just run 1
```

## Project Structure

```
src/
  main/
    java/               # Source code
  resources/            # Configuration files
  test/
    integration-test/   # Integration tests
    java/               # Unit tests
conf/                   # Router configuration files
target/                 # Compiled classes (generated)
```

## PA1 Implementation

### CLI

The CLI is the primary module for all command line related operations, including argument parsing, validation and sdout
display. We refactored `terminal` out of `Router` into this class.

It contains a `Console` class, which runs on its own thread and is responsible for any text displayed on the terminal.
This was done to consolidate it as a single source of truth for text display, and also manage the state of the frontend
when an attach request comes in; When a router an attach request, it must display a modal and take user input for
accepting/rejecting the input, therefore this state must be managed by itself.

### Router

Router is the primary class. It manages a server thread (using a light wrapper `RouterTransport`) that services all
incoming requests from unknown routers (for now, only attach requests come from unknown routers).
It also is the entry point for every command after CLI has done validation. When processing a command, the Router class
uses the correct dependencies to fulfill the demand.

### PortsTable

We refactored out the ports array into a ports table for all operations, so the Router does not have to manage a raw
array. This also makes synchronization easier.

The port table manages a fixed numbers of ports, each port can contain a `Link`.

For now we just made all mutable functions synchronized. For better performance, we may read a read write lock in the
future to save time on read operations.

### Link

Each link contains the information of our router and the router that we are attached to, as well as an open socket to
maintain persistent peer to peer connection to the attached router. Each link contains a `Channel`. When listening for
messages, it uses a specific handler for neighbour communication.

Each link runs its own thread that listens and services requests from neighbouring routers.

### Channel

Channel manages the I/O of a specific socket, it lightly wraps a `Socket` class to abstract sending and receiving a
`SOSPFPPacket`.

## Assumptions

We also made some assupmptions on the behaviour of the application for edge cases.

1. We assume that when a router is being asked for attach, other requests that come in get auto rejected until the
   router makes a y/n choice.
2. We assume that all routers will use our implementation, i.e. the packet will always follow the SOSPFPacket protocol
   implemented in this repo
3. We did not implement much handling for connection issues, since it wasn't specified the protocol for it right now. If
   an
   attached router goes down (socket closed), for now the application just prints the error (EOFexception) out.
   .

## PA2 Implementation

### LSAUpdate

LSAUpdate gets triggered on a couple scenarios

- Running `start`: Synchronizes LSA for current TWO_WAY neighbours
- Receiving DBSyncHello: This implies that a neighbour may have turned TWO_WAY, which means we must synchronize LSAs/
- Receiving an LSAUpdate: Cross-check with current LSD, if the LSA from the router that sent the update differs in seq
  number, update local LSD to match the new version.

### Detect

This runs the shortest pathfinding algorithm and returns the path

### Send

At the start router, it

1. Runs the shortest pathfinding algorithm
2. Extracts the next hop
3. Send the packet to the next hop
4. Repeat 1-3 until it reaches the destination IP

We do hop-by-hop routing because this is good at handling if the network topology changes mid-travel compared to source
routing. The downside is that it is less efficient to run the pathfinding at every hop.

## PA3 Implementations

### Connect

We added a flag for whether the router has already ran `start`, if it has, we allow it to run the `connect` command.

The connect command simply calls `processAttach` and `processStart` to run both operations at the same time on a single new neighbouring router.

### Update

When a router X runs the update command for the weight of the edge to router Y, it makes changes in its LSD and ports to update the weight:

- X's port table needs a new updated weight to Y
- In X's LSD, X's LSA needs a new updated weight to Y
- In X's LSD, Y's LSA needs a new updated weight toX

After these changes are made, X sends an LSAUpdate to neighbours.

When Y receive X's LSAUpdate, it will see that its own LSA's sequence number received in the packet is higher than its local LSD, and know that a weight may have been changed. It will iterate it and update its LSD and ports table accordingly.

### Disconnect

When router X runs `disconnect`, it sends a dedicated `DISCONNECT` packet to router Y before closing the local link.

When Y receives `DISCONNECT`, it removes the link to X and closes the socket on its side too. This lets both routers
gracefully tear down the connection without treating it as a router crash.

After link teardown on both sides, each router synchronizes and broadcasts its LSD so the topology converges to the new
graph.

### Quit

On quit, a router clears its own LSA and sends it to neighbours to prompt an update removing all links, then sends a special `EXIT` packet to notify them that it is going down, then exits.

When neighbours receive the `EXIT` packet, they close the socket with the router that went down, then broadcasts to all neighbours that said router went down. Neighbours receiving this special packet will remove that router from their LSD. This is important because if that router ever comes back up but restarts its LSA sequence number count, it must be able to cleanly re-instante itself the networks LSD.

An alternative design to this is to keep a router's sequence number on-disk so that it never decrements on reboot. I opted for the former design for ease of demo.
