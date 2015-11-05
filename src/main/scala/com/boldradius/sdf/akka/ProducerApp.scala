package com.boldradius.sdf.akka

import akka.actor._
import com.boldradius.sdf.akka.RequestProducer._
import com.typesafe.config.{ConfigFactory}
import scala.io.StdIn
import scala.concurrent.duration._

object ProducerApp {
  def main(args: Array[String]): Unit = {
    new ProducerApp(ActorSystem("ClusterSystem", ConfigFactory.load("producer")).run()
  }
}

class ProducerApp(system: ActorSystem) {
  val statsAggregator = PdAkkaActor.createActor(system, StatsAggregator.Args, Some("statsAggregator"))
  val producer = system.actorOf(RequestProducer.props(100), "producerActor")
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
