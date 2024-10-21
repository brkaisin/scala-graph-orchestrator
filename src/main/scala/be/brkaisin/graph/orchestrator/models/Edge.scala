package be.brkaisin.graph.orchestrator.models

import Node.NodeId

/** Represents an edge in a graph.
  * @param from
  *   the unique identifier of the source node
  * @param to
  *   the unique identifier of the target node
  */
case class Edge(from: NodeId, to: NodeId)
