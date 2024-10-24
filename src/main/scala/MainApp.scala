import be.brkaisin.graph.orchestrator.core.Orchestrator
import be.brkaisin.graph.orchestrator.core.Orchestrator.Command.*
import be.brkaisin.graph.orchestrator.core.Orchestrator.Confirmation.*
import be.brkaisin.graph.orchestrator.models.Node.NodeId
import be.brkaisin.graph.orchestrator.models.{Edge, Node}
import be.brkaisin.graph.orchestrator.utils.OptionFields
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import zio.*

object MainApp extends App:

  given trace: Trace = Trace.empty

  // Define case classes for node inputs with Option fields
  case class NodeAInput(value: Option[String]) extends Product
  case class NodeBInput(value: Option[Int]) extends Product
  case class NodeCInput(value: Option[Double]) extends Product
  case class NodeDInput(intValue: Option[Int], doubleValue: Option[Double])
      extends Product

  // Define nodes with case classes as input
  val nodeA: Node[NodeAInput, Int, Throwable] =
    Node[NodeAInput, Int, Throwable](
      NodeId("A"),
      input => ZIO.succeed(input.value.getOrElse("").length).delay(1.second)
    )

  val nodeB: Node[NodeBInput, Double, Throwable] =
    Node[NodeBInput, Double, Throwable](
      NodeId("B"),
      input =>
        ZIO.succeed(input.value.getOrElse(0).toDouble * 2.0).delay(2.seconds)
    )

  val nodeC: Node[NodeCInput, Int, Throwable] =
    Node[NodeCInput, Int, Throwable](
      NodeId("C"),
      input => ZIO.succeed(input.value.getOrElse(0.0).toInt)
    )

  // New node with multiple inputs (Node D)
  val nodeD: Node[NodeDInput, String, Throwable] =
    Node[NodeDInput, String, Throwable](
      NodeId("D"),
      input =>
        ZIO
          .succeed {
            val intPart    = input.intValue.getOrElse(0)
            val doublePart = input.doubleValue.getOrElse(0.0)
            s"Processed values: int = $intPart, double = $doublePart"
          }
          .delay(1.second)
    )

  Unsafe.unsafe { implicit unsafe =>
    val orchestratorSystem = ActorSystem(Orchestrator(), "orchestrator")

    val confirmationActor: ActorRef[Orchestrator.Confirmation] =
      orchestratorSystem.systemActorOf(
        Behaviors.receiveMessage { Ack =>
          println("Received Ack for operation.")
          Behaviors.same
        },
        "confirmation-actor"
      )

    // Add nodes and edges
    orchestratorSystem ! AddNode(nodeA, confirmationActor)
    orchestratorSystem ! AddNode(nodeB, confirmationActor)
    orchestratorSystem ! AddNode(nodeC, confirmationActor)
    orchestratorSystem ! AddNode(nodeD, confirmationActor)

    orchestratorSystem ! AddEdge(
      Edge(nodeA.id, nodeB.id, 0),
      confirmationActor
    )

    orchestratorSystem ! AddEdge(
      Edge(nodeB.id, nodeC.id, 0),
      confirmationActor
    )

    // Link outputs of Node B and Node C to Node D as different fields
    orchestratorSystem ! AddEdge(
      Edge(nodeC.id, nodeD.id, 0),
      confirmationActor
    )

    orchestratorSystem ! AddEdge(
      Edge(nodeB.id, nodeD.id, 1),
      confirmationActor
    )

    // Execute Node A with case class input
    orchestratorSystem ! ExecuteNode(
      nodeA,
      NodeAInput(Some("Hello")),
      confirmationActor
    )

    // Wait to allow the processing to complete before the application exits
    Thread.sleep(10000)
  }
