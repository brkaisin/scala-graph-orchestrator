package be.brkaisin.graph.orchestrator.models

import Node.NodeId
import zio.ZIO
import be.brkaisin.graph.orchestrator.utils.OptionFields

/** Represents a node in a graph.
  * @param id
  *   the unique identifier of the node
  * @param compute
  *   the function that computes the output of the node given an input
  * @tparam I
  *   the input type of the node
  * @tparam O
  *   the output type of the node
  * @tparam E
  *   the error type of the node
  */
case class Node[-I <: Product, +O, +E](
    id: NodeId,
    compute: I => ZIO[Any, E, O]
)

object Node:
  opaque type NodeId = String

  object NodeId:
    def apply(value: String): NodeId             = value
    extension (nodeId: NodeId) def value: String = nodeId
