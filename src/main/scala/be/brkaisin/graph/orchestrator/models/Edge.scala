package be.brkaisin.graph.orchestrator.models

import Node.NodeId

/** Represents an edge in a graph.
  * @param from
  *   the unique identifier of the source node
  * @param to
  *   the unique identifier of the target node
  * @param toFieldIndex
  *   the index of the field in the target node to which the output should be
  *   sent
  */
case class Edge(from: NodeId, to: NodeId, toFieldIndex: Int)
