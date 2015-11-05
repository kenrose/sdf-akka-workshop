package com.boldradius.sdf.akka

import akka.actor._
import com.boldradius.sdf.akka.RequestProducer._
import com.typesafe.config.{ConfigFactory}
import scala.io.StdIn
import scala.concurrent.duration._

object ProducerApp {
  def main(args: Array[String]): Unit = {
    new ProducerApp(ActorSystem("ClusterSystem", ConfigFactory.load("producer"))).run()
  }
}

class ProducerApp(system: ActorSystem) {
  val statsAggregator = PdAkkaActor.createActor(system, StatsAggregator.Args, Some("statsAggregator"))
  val producer = system.actorOf(RequestProducer.props(100), "producerActor")
  val consumer = PdAkkaActor.createActor(system, Consumer.Args(statsAggregator), Some("consumer"))

  def run(): Unit = {
    producer ! Start(consumer)
  }
}
