import be.brkaisin.graph.orchestrator.core.Orchestrator
import be.brkaisin.graph.orchestrator.core.Orchestrator.Command.*
import be.brkaisin.graph.orchestrator.models.Node.NodeId
import be.brkaisin.graph.orchestrator.models.{Edge, Node}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import zio.*
import scala.language.implicitConversions

object MainApp extends App:

  given trace: Trace = Trace.empty

  extension [T](tuple1: Tuple1[T]) implicit inline def extract: T = tuple1._1

  case class NodeAInput(value: String)

  val nodeA: Node[Tuple1[String], Int, Throwable] =
    Node.withCaseClassInput[NodeAInput, Int, Throwable](
      NodeId("A"),
      input => ZIO.succeed(input.value.length).delay(1.second)
    )

  val nodeB: Node[Tuple1[Int], Double, Throwable] =
    Node(
      NodeId("B"),
      input => ZIO.succeed(input * 2.0).delay(2.seconds)
    )

  val nodeC: Node[Tuple1[Double], Int, Throwable] =
    Node(
      NodeId("C"),
      input => ZIO.succeed(input.toInt).delay(1.second)
    )

  // new node with multiple inputs
  val nodeD: Node[(Int, Double), String, Throwable] =
    Node(
      NodeId("D"),
      (int, double) =>
        ZIO
          .succeed {
            s"Processed values: int = $int, double = $double"
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
      Tuple1("Hello"),
      confirmationActor
    )

    // wait to allow the processing to complete before the application exits
    Thread.sleep(10000)
  }
