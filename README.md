# Scala Graph Orchestrator

The **Graph Orchestrator** is a system for managing, executing, and orchestrating the interactions between nodes in a 
directed graph structure. Each node represents a computational unit, and edges define the dependencies between them. 
This project is built on top of the [Pekko](https://pekko.apache.org/) Actor system for concurrency and fault tolerance,
and uses the [ZIO](https://zio.dev/) library for functional effect handling.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
    - [Orchestrator](#orchestrator)
    - [NodeActor](#nodeactor)
    - [Graph](#graph)
    - [Node](#node)
    - [Edge](#edge)
- [Usage](#usage)
- [Logging](#logging)
- [Installation](#installation)
- [Contributing](#contributing)
- [License](#license)
- [To-Do](#to-do)

## Overview

This project implements an orchestrator that manages computational nodes arranged as a directed graph. Nodes can execute 
tasks, pause, resume, and handle dependency relationships with other nodes. The project leverages Pekko's actor model 
for concurrency and scalability, while ZIO is used to handle asynchronous computations safely and functionally.

### Key Features

- **Node Management:** Add, remove, pause, and resume nodes dynamically.
- **Execution Control:** Execute nodes based on input and propagate results to dependent nodes.
- **Logging:** Rich logging support via Pekko's logging system for monitoring execution and failures.
- **Error Handling:** Handle computation failures in nodes gracefully.
- **Graph Updates:** Dynamically update the dependencies of nodes within the graph.

## Architecture

The project consists of several key components:

### Orchestrator

The **Orchestrator** actor is responsible for managing the overall graph structure and node actors. It handles the 
following tasks:

- **AddNode:** Add a new node to the graph.
- **RemoveNode:** Remove a node from the graph.
- **AddEdge:** Add a directed edge (dependency) between two nodes.
- **ExecuteNode:** Trigger the execution of a node.
- **PauseNode/ResumeNode:** Pause or resume a node’s execution.

The orchestrator also ensures that node dependencies are updated correctly when the graph structure changes.

### NodeActor

Each node is represented by a **NodeActor**, which is responsible for executing its computation and sending the result 
to dependent nodes. It supports the following:

- **Input:** Receive an input value and compute the output.
- **Pause/Resume:** Control the state of the node (active or paused).
- **UpdateDependencies:** Update its dependent nodes based on the current graph structure.

The node actor interacts with ZIO for asynchronous computations and relies on Pekko’s actor system to handle message 
passing and state transitions.

### Graph

The **Graph** represents the structure of nodes and edges. It provides methods to add and remove nodes and edges, and 
to find dependent nodes of a given node.

### Node

A **Node** represents a unit of computation with the following attributes:

- **ID:** A unique identifier.
- **Compute Function:** A function that takes input and produces output asynchronously.
- **Dependencies:** A list of nodes whose outputs are required as inputs.

### Edge

An **Edge** represents a directed connection between two nodes, where the output of one node becomes the input for 
another.

## Usage

### Example Flow

1. **Add a node** to the orchestrator:
    ```scala
    orchestrator ! AddNode(node, replyTo)
    ```
2. **Add an edge** (dependency) between two nodes:
    ```scala
    orchestrator ! AddEdge(edge, replyTo)
    ```
3. **Execute a node** with a specific input:
    ```scala
    orchestrator ! ExecuteNode(node, input, replyTo)
    ```
4. **Pause/Resume** a node:
    ```scala
    orchestrator ! PauseNode(nodeId, replyTo)
    orchestrator ! ResumeNode(nodeId, replyTo)
    ```

### Logging

Logging is integrated into the system to monitor various actions, such as adding nodes, executing nodes, and handling 
errors. The `context.log.info` and `context.log.error` methods are used to track the system's behavior.

For example:
- When a node is executed:
  ```
  Node {nodeId} executed successfully with output: {output}
  ```
- When an error occurs:
  ```
  Node {nodeId} failed with error: {error}
  ```

### Error Handling

Errors during computation are handled by the node actors. If a node fails, the orchestrator is notified and an 
appropriate error message is logged.

## Installation

To run the project, ensure that you have [sbt](https://www.scala-sbt.org/) and Java installed on your system. 
system.

### Steps:

1. Clone this repository:
   ```bash
   git clone https://github.com/brkaisin/scala-graph-orchestrator.git
   ```
2. Navigate to the project directory:
   ```bash
   cd scala-graph-orchestrator
   ```
3. Build/Run the project:
   ```bash
   sbt run
   ```

## Contributing

Contributions are welcome! To contribute:

1. Fork the repository.
2. Create a feature branch.
3. Commit your changes and push the branch.
4. Open a pull request.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE.txt) file for more details.

## To-Do

- [ ] **Write tests:** Add unit and integration tests.
- [ ] **Handle multiple input compositions on a node:** Extend node logic to handle multiple input streams or 
compositions.
- [ ] **Error handling and retry policies:** Implement robust error-handling mechanisms and configurable retry policies.
- [ ] **Monitoring and real-time visualization of the graph:** Integrate tools to monitor and visualize the graph 
execution in real-time.
- [ ] **Distribute the architecture with Pekko Cluster:** Scale the system by distributing nodes across a Pekko Cluster.
- [ ] **Support for gRPC and other asynchronous protocols:** Add gRPC and other remote communication protocols for 
distributed node interactions.
- [ ] **Documentation and examples:** Provide thorough documentation and code examples for developers.
