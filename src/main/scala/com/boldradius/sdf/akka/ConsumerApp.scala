package com.boldradius.sdf.akka

import com.typesafe.config.{ConfigFactory}
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._

object ConsumerApp {
  def main(args: Array[String]): Unit = {
    new ConsumerApp(ActorSystem("ClusterSystem", ConfigFactory.load("consumer")))
  }
}

class ConsumerApp(system: ActorSystem) {
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

  val statsAggregator = createSupervisedActor(StatsAggregator.Args, "statsAggregator")
  val consumer = PdAkkaActor.createActor(system, Consumer.Args(statsAggregator), Some("consumer"))
}
