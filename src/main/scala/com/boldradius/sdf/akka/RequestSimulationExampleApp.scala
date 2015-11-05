package com.boldradius.sdf.akka

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.boldradius.sdf.akka.RequestProducer._
import scala.io.StdIn
import scala.concurrent.Await
import scala.concurrent.duration._

object RequestSimulationExampleApp {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("EventProducerExample")
    val app = new RequestSimulationExampleApp(system)
    app.run()
  }
}

class RequestSimulationExampleApp(system: ActorSystem) {
  val settings = Settings(system)
  val emailSender = PdAkkaActor.createActor(system, EmailActor.Args, Some("emailer"))
  // creates a supervised actor
  def createSupervisedActor(
      subordinateArgs: PdAkkaActor.Args,
      subordinateName: String): ActorRef = {
    val supervisor = PdAkkaActor.createActor(system,
      Supervisor.Args(subordinateArgs, subordinateName, emailSender),
      Some(s"supervisor-$subordinateName"))

    implicit val executionContext = system.dispatcher
    implicit val timeout: Timeout = settings.SUPERVISOR_STARTUP_TIMEOUT
    val res = (supervisor ? Supervisor.GetSubordinate).mapTo[Supervisor.Subordinate]
    Await.result(res, Duration.Inf).subordinate
  }

  val producer = system.actorOf(RequestProducer.props(100), "producerActor")

  val statsAggregator = createSupervisedActor(StatsAggregator.Args, "statsAggregator")
  val consumer = PdAkkaActor.createActor(system, Consumer.Args(statsAggregator), Some("consumer"))

  def run(): Unit = {
    // Tell the producer to start working and to send messages to the consumer
    producer ! Start(consumer)

    // Wait for the user to hit <enter>
    println("Hit <enter> to stop the simulation")
    StdIn.readLine()

    // Tell the producer to stop working
    producer ! Stop

    // Terminate all actors and wait for graceful shutdown
    system.shutdown()
    system.awaitTermination(10 seconds)
  }
}
