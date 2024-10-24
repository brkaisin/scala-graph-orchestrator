package be.brkaisin.graph.orchestrator.models

import Node.NodeId

/** A graph is a collection of nodes and edges.
  * @param nodes
  *   the nodes in the graph
  * @param edges
  *   the edges in the graph
  */
case class Graph(
    nodes: Map[NodeId, Node[?, ?, ?]],
    edges: List[Edge]
):

  def addNode[I <: Product, O, E](node: Node[I, O, E]): Graph =
    copy(nodes = nodes + (node.id -> node))

  def removeNode(id: NodeId): Graph =
    copy(
      nodes = nodes - id,
      edges = edges.filterNot(edge => edge.from == id || edge.to == id)
    )

  def addEdge(edge: Edge): Graph =
    copy(edges = edge :: edges)

  def findDependentEdges(id: NodeId): List[Edge] =
    edges.filter(_.from == id)

object Graph:
  def empty: Graph = Graph(Map.empty, List.empty)
