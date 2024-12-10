package be.brkaisin.graph.orchestrator.models

import be.brkaisin.graph.orchestrator.globals.{getTupleOptions, OptionsTuple}
import be.brkaisin.graph.orchestrator.models.Node.NodeId
import be.brkaisin.graph.orchestrator.utils.OptionFields
import zio.ZIO

import scala.deriving.Mirror

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
case class Node[-I <: Tuple, +O, +E](
    id: NodeId,
    compute: I => ZIO[Any, E, O]
):
  def contraMap[I2 <: Tuple](f: I2 => I): Node[I2, O, E] =
    Node(id, input => compute(f(input)))

object Node:
  opaque type NodeId = String

  object NodeId:
    def apply(value: String): NodeId             = value
    extension (nodeId: NodeId) def value: String = nodeId

  def liftInputToOptions[I <: Tuple, O, E](
      node: Node[I, O, E]
  )(using OptionFields[OptionsTuple[I]]): Node[OptionsTuple[I], O, E] =
    node.contraMap(getTupleOptions)

  def withCaseClassInput[P <: Product, O, E](
      id: NodeId,
      compute: P => ZIO[Any, E, O]
  )(using m: Mirror.ProductOf[P]): Node[m.MirroredElemTypes, O, E] =
    Node(
      id,
      (input: m.MirroredElemTypes) => compute(m.fromProduct(input))
    )
