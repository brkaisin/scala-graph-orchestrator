package be.brkaisin.graph.orchestrator.core

import be.brkaisin.graph.orchestrator.models.Node.NodeId
import be.brkaisin.graph.orchestrator.models.NodeResult.*
import be.brkaisin.graph.orchestrator.models.{Graph, Node, NodeResult}
import be.brkaisin.graph.orchestrator.utils.OptionFields
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import zio.*

object NodeActor:

  /** Commands handled by the NodeActor */
  enum Command:
    case InputField[T](index: Int, value: T)
    case Pause
    case Resume
    case UpdateDependencies(
        graph: Graph,
        nodeActors: Map[NodeId, ActorRef[Command]]
    )

  import Command.*

  /** Actor's state representing the accumulated input */
  case class AccumulatedInput[I <: Product](input: I)

  /** Applies the NodeActor behavior */
  def apply[I <: Product, O, E](
      node: Node[I, O, E],
      dependencies: List[(ActorRef[InputField[O]], Int)],
      replyTo: ActorRef[NodeResult[O, E]]
  )(using
      trace: Trace,
      unsafe: Unsafe,
      fields: OptionFields[I]
  ): Behavior[Command] =
    running(node, dependencies, replyTo, AccumulatedInput(fields.empty))

  /** Behavior of the NodeActor when it's running */
  def running[I <: Product, O, E](
      node: Node[I, O, E],
      dependencies: List[(ActorRef[InputField[O]], Int)],
      replyTo: ActorRef[NodeResult[O, E]],
      accumulated: AccumulatedInput[I]
  )(using
      trace: Trace,
      unsafe: Unsafe,
      fields: OptionFields[I]
  ): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match
        case InputField(index, value) =>
          context.log.info(s"Node ${node.id} received field $index: $value")

          // Ensure that if the incoming value isn't an Option, wrap it in one
          val wrappedValue: Option[?] = value match
            case option: Option[?] => option
            case _                 => Option(value)

          val updatedInput =
            fields.mergeField(accumulated.input, index, wrappedValue)

          if fields.isComplete(updatedInput) then
            context.log.info(
              s"Node ${node.id} received complete input: $updatedInput"
            )
            Runtime.default.unsafe
              .run(
                node.compute(updatedInput).either
              ) match
              case Exit.Success(Right(output)) =>
                context.log.info(
                  s"Node ${node.id} executed successfully with output: $output"
                )
                // Send the output to the appropriate field index in dependent nodes
                dependencies.foreach { case (actorRef, fieldIndex) =>
                  actorRef ! InputField(fieldIndex, output)
                }
                replyTo ! Success(output)
              case Exit.Success(Left(error)) =>
                context.log.error(s"Node ${node.id} failed with error: $error")
                replyTo ! Failure(error)
              case Exit.Failure(cause) =>
                context.log.error(
                  s"Node ${node.id} failed with cause: ${cause.squash}"
                )
          // This cannot happen, we have a Cause[Nothing]

          running(node, dependencies, replyTo, AccumulatedInput(updatedInput))

        case Pause =>
          context.log.info(s"Node ${node.id} paused.")
          paused(node, dependencies, replyTo, accumulated)

        case UpdateDependencies(graph, nodeActors) =>
          context.log.info(s"Node ${node.id} updating dependencies.")
          val newDependencies =
            graph.findDependentEdges(node.id).flatMap { edge =>
              nodeActors.get(edge.to).map { actorRef =>
                (actorRef, edge.toFieldIndex)
              }
            }

          context.log.info(
            s"Node ${node.id} updated dependencies: ${newDependencies.map(_._1)}"
          )
          running(node, newDependencies, replyTo, accumulated)

        case _ =>
          Behaviors.same
    }

  /** Behavior of the NodeActor when it's paused */
  def paused[I <: Product, O, E](
      node: Node[I, O, E],
      dependencies: List[(ActorRef[InputField[O]], Int)],
      replyTo: ActorRef[NodeResult[O, E]],
      accumulated: AccumulatedInput[I]
  )(using
      trace: Trace,
      unsafe: Unsafe,
      fields: OptionFields[I]
  ): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match
        case Resume =>
          context.log.info(s"Node ${node.id} resumed.")
          running(node, dependencies, replyTo, accumulated)

        case _ =>
          Behaviors.same
    }
