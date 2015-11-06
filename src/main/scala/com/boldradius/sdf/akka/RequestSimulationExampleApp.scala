package com.boldradius.sdf.akka

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.io.StdIn
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.annotation.tailrec
import scala.util.{Try, Success}

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
    val maxRestarts = settings.SUPERVISOR_RESTART_COUNT
    val supervisor = PdAkkaActor.createActor(system,
      Supervisor.Args(subordinateArgs, subordinateName, emailSender, maxRestarts),
      Some(s"supervisor-$subordinateName"))

    implicit val executionContext = system.dispatcher
    implicit val timeout: Timeout = settings.SUPERVISOR_STARTUP_TIMEOUT
    val res = (supervisor ? Supervisor.GetSubordinate).mapTo[Supervisor.Subordinate]
    Await.result(res, Duration.Inf).subordinate
  }

  val producer = PdAkkaActor.createActor(system, RequestProducer.Args(100, None), Some("producerActor"))

  val statsAggregator = createSupervisedActor(StatsAggregator.Args, "statsAggregator")
  val consumer = PdAkkaActor.createActor(system, Consumer.Args, Some("consumer"))

  def run(): Unit = {
    // Tell the producer to start working and to send messages to the consumer
    // producer ! Start(consumer)
  }

  @tailrec
  private def commandLoop(): Unit = {
    val line = StdIn.readLine()
    Try(line.toInt) match {
      case Success(numExplosions) => {
        (1 to numExplosions).foreach { _ => (statsAggregator ! Explode)}
        println(s"Sent ${numExplosions} explosions to StatsAggregator")
        commandLoop()
      }
      case _ => ()
    }
  }
}
