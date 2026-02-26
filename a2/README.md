# COMP535 Project Starter Code

## Requirements

To compile and run this project, you need:

- **Java**: Version 8 or higher (JDK 8+)
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

```just
just run conf/router1.conf
```

## Implementation

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
3. We did not implement much handling for connection issues, since it wasn't specified the protocol for it right now. If an
   attached router goes down (socket closed), for now the application just prints the error (EOFexception) out.

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
