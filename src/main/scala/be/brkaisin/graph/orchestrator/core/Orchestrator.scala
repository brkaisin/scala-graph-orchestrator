package be.brkaisin.graph.orchestrator.core

import be.brkaisin.graph.orchestrator.models.Node.NodeId
import be.brkaisin.graph.orchestrator.models.NodeResult.*
import be.brkaisin.graph.orchestrator.models.{Edge, Graph, Node, NodeResult}
import be.brkaisin.graph.orchestrator.utils.OptionFields
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import zio.*

object Orchestrator:

  enum Command:
    case AddNode[I <: Product, O, E](
        node: Node[I, O, E],
        replyTo: ActorRef[Confirmation]
    )(using val optionFields: OptionFields[I])
    case RemoveNode(
        id: NodeId,
        replyTo: ActorRef[Confirmation]
    )
    case ExecuteNode[I <: Product, O, E](
        node: Node[I, O, E],
        input: I,
        replyTo: ActorRef[Confirmation]
    )
    case AddEdge(
        edge: Edge,
        replyTo: ActorRef[Confirmation]
    )
    case PauseNode(
        id: NodeId,
        replyTo: ActorRef[Confirmation]
    )
    case ResumeNode(
        id: NodeId,
        replyTo: ActorRef[Confirmation]
    )
    case ExecutionResult[O, E](
        result: NodeResult[O, E],
        nodeId: NodeId
    )

  enum Confirmation:
    case Ack

  case class OrchestratorState(
      graph: Graph,
      nodeActors: Map[NodeId, ActorRef[NodeActor.Command]]
  )

  import Command.*
  import NodeActor.Command.*
  import Confirmation.*

  def apply(
      state: OrchestratorState = OrchestratorState(Graph.empty, Map.empty)
  )(using trace: Trace, unsafe: Unsafe): Behavior[Command] = Behaviors.setup {
    context =>
      Behaviors.receiveMessage {
        case command @ AddNode(node, replyTo) =>
          import command.optionFields
          context.log.info(s"Adding node with ID: ${node.id}")

          val resultAdapter: ActorRef[NodeResult[?, ?]] =
            context.spawn(
              Behaviors.receiveMessage { nodeResult =>
                context.self ! ExecutionResult(nodeResult, node.id)
                Behaviors.same
              },
              s"result-collector-${node.id}"
            )

          // spawn node actor without dependencies (they will be updated later)
          val nodeActor = context.spawn(
            NodeActor(node, List.empty, resultAdapter),
            s"node-${node.id}"
          )

          val updatedGraph  = state.graph.addNode(node)
          val updatedActors = state.nodeActors + (node.id -> nodeActor)

          updateDependencies(updatedGraph, updatedActors)

          replyTo ! Ack
          apply(state.copy(graph = updatedGraph, nodeActors = updatedActors))

        case RemoveNode(id, replyTo) =>
          context.log.info(s"Removing node with ID: $id")

          state.nodeActors.get(id).foreach(context.stop)
          val updatedGraph  = state.graph.removeNode(id)
          val updatedActors = state.nodeActors - id

          updateDependencies(updatedGraph, updatedActors)

          replyTo ! Ack
          apply(state.copy(graph = updatedGraph, nodeActors = updatedActors))

        case ExecuteNode(node, input, replyTo) =>
          context.log.info(
            s"Executing node with ID: ${node.id} and input: $input"
          )

          state.nodeActors.get(node.id).foreach { actor =>
            // Send each field of the input separately as an InputField command
            val inputIterator = input.productIterator.zipWithIndex
            inputIterator.foreach { case (field, index) =>
              actor ! InputField(index, field)
            }
          }
          replyTo ! Ack
          Behaviors.same

        case AddEdge(edge, replyTo) =>
          context.log.info(s"Adding edge from ${edge.from} to ${edge.to}")

          val updatedGraph = state.graph.addEdge(edge)

          updateDependencies(updatedGraph, state.nodeActors)

          replyTo ! Ack
          apply(state.copy(graph = updatedGraph))

        case PauseNode(id, replyTo) =>
          context.log.info(s"Pausing node with ID: $id")

          state.nodeActors.get(id).foreach(_ ! Pause)
          replyTo ! Ack
          Behaviors.same

        case ResumeNode(id, replyTo) =>
          context.log.info(s"Resuming node with ID: $id")

          state.nodeActors.get(id).foreach(_ ! Resume)
          replyTo ! Ack
          Behaviors.same

        case ExecutionResult(result, nodeId) =>
          result match
            case Success(output) =>
              context.log.info(
                s"Node $nodeId successfully announced its output: $output"
              )
            case Failure(error) =>
              context.log.error(
                s"Node $nodeId successfully announced a failure: $error"
              )
          Behaviors.same
      }
  }

  def updateDependencies(
      graph: Graph,
      nodeActors: Map[NodeId, ActorRef[NodeActor.Command]]
  ): Unit =
    nodeActors.values.foreach { actor =>
      actor ! UpdateDependencies(graph, nodeActors)
    }
