package com.boldradius.sdf.akka

import akka.actor._
import com.boldradius.sdf.akka.RequestProducer._
import scala.io.StdIn
import scala.concurrent.duration._

object RequestSimulationExampleApp {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("EventProducerExample")
    val app = new RequestSimulationExampleApp(system)
    app.run()
  }
}

class RequestSimulationExampleApp(system: ActorSystem) {
  val statsAggregator = PdAkkaActor.createActor(system, StatsAggregator.Args, Some("statsAggregator"))
  val producer = system.actorOf(RequestProducer.props(100), "producerActor")
  val consumer = PdAkkaActor.createActor(system, Consumer.Args(statsAggregator), Some("consumer"))

  def run(): Unit = {
    // Tell the producer to start working and to send messages to the consumer
    producer ! Start(consumer)
  }
}
