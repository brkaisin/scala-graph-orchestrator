import be.brkaisin.graph.orchestrator.core.Orchestrator
import be.brkaisin.graph.orchestrator.core.Orchestrator.Command.*
import be.brkaisin.graph.orchestrator.core.Orchestrator.Confirmation.*
import be.brkaisin.graph.orchestrator.models.Node.NodeId
import be.brkaisin.graph.orchestrator.models.{Edge, Node}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import zio.*

object MainApp extends App:

  given trace: Trace = Trace.empty

  val nodeA: Node[String, Int, Throwable] =
    Node[String, Int, Throwable](
      NodeId("A"),
      input => ZIO.succeed(input.length).delay(1.second)
    )

  val nodeB: Node[Int, Double, Throwable] =
    Node[Int, Double, Throwable](
      NodeId("B"),
      input => ZIO.succeed(input.toDouble * 2.0).delay(2.seconds)
    )

  val nodeC: Node[Double, Double, Throwable] =
    Node[Double, Double, Throwable](
      NodeId("C"),
      input => ZIO.succeed(input * 3.0)
    )

  Unsafe.unsafe { implicit unsafe =>
    val orchestratorSystem = ActorSystem(Orchestrator(), "orchestrator")

    val confirmationActor: ActorRef[Orchestrator.Confirmation] =
      orchestratorSystem.systemActorOf(
        Behaviors.receive { case (context, Ack) =>
          context.log.info("Received Ack for operation.")
          Behaviors.same
        },
        "confirmation-actor"
      )

    orchestratorSystem ! AddNode(nodeA, confirmationActor)
    orchestratorSystem ! AddNode(nodeB, confirmationActor)
    orchestratorSystem ! AddEdge(
      Edge(nodeA.id, nodeB.id),
      confirmationActor
    )

    orchestratorSystem ! ExecuteNode(
      nodeA,
      "Hello",
      confirmationActor
    )

    Thread.sleep(1000)

    orchestratorSystem ! AddNode(nodeC, confirmationActor)
    orchestratorSystem ! AddEdge(
      Edge(nodeB.id, nodeC.id),
      confirmationActor
    )
  }
