package com.boldradius.sdf.akka

import akka.actor._
import com.boldradius.sdf.akka.RequestProducer._
import com.typesafe.config.{ConfigFactory}
import scala.io.StdIn
import scala.concurrent.duration._

object ProducerApp {
  def main(args: Array[String]): Unit = {
    new ProducerApp(ActorSystem("ClusterSystem", ConfigFactory.load("producer")))
  }
}

class ProducerApp(system: ActorSystem) {
  val producer = system.actorOf(RequestProducer.props(100))
}
