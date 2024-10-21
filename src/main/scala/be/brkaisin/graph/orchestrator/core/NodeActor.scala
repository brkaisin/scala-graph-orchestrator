package be.brkaisin.graph.orchestrator.core

import be.brkaisin.graph.orchestrator.models.Node.NodeId
import be.brkaisin.graph.orchestrator.models.NodeResult.*
import be.brkaisin.graph.orchestrator.models.{Graph, Node, NodeResult}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import zio.*

object NodeActor:

  enum Command:
    case Input[I](value: I)
    case Pause
    case Resume
    case UpdateDependencies(
        graph: Graph,
        nodeActors: Map[NodeId, ActorRef[Command]]
    )

  import Command.*

  def apply[I, O, E](
      node: Node[I, O, E],
      dependencies: List[ActorRef[Input[O]]],
      replyTo: ActorRef[NodeResult[O, E]]
  )(using trace: Trace, unsafe: Unsafe): Behavior[Command] =
    running(node, dependencies, replyTo)

  def running[I, O, E](
      node: Node[I, O, E],
      dependencies: List[ActorRef[Input[O]]],
      replyTo: ActorRef[NodeResult[O, E]]
  )(using trace: Trace, unsafe: Unsafe): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match
        case Input(input) =>
          context.log.info(s"Node ${node.id} received input: $input")

          Runtime.default.unsafe
            .run(
              node.compute(input.asInstanceOf[I]).either
            ) match
            case Exit.Success(Right(output)) =>
              context.log.info(
                s"Node ${node.id} executed successfully with output: $output"
              )
              dependencies.foreach(_ ! Input(output))
              replyTo ! Success(output)
            case Exit.Success(Left(error)) =>
              context.log.error(s"Node ${node.id} failed with error: $error")
              replyTo ! Failure(error)
            case _ => // this cannot happen, we have a Cause[Nothing]
          Behaviors.same

        case Pause =>
          context.log.info(s"Node ${node.id} paused.")
          paused(node, dependencies, replyTo)

        case UpdateDependencies(graph, nodeActors) =>
          context.log.info(s"Node ${node.id} updating dependencies.")
          // Find new dependencies from the graph
          val dependentNodeIds = graph.findDependentNodes(node.id)
          val newDependencies  = dependentNodeIds.flatMap(nodeActors.get)

          context.log.info(
            s"Node ${node.id} updated dependencies: $dependentNodeIds"
          )
          running(node, newDependencies, replyTo)

        case _ =>
          Behaviors.same
    }

  def paused[I, O, E](
      node: Node[I, O, E],
      dependencies: List[ActorRef[Input[O]]],
      replyTo: ActorRef[NodeResult[O, E]]
  )(using trace: Trace, unsafe: Unsafe): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match
        case Resume =>
          context.log.info(s"Node ${node.id} resumed.")
          running(node, dependencies, replyTo)

        case _ =>
          Behaviors.same
    }
