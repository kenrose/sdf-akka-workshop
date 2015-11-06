package com.boldradius.sdf.akka

import akka.contrib.pattern.{DistributedPubSubExtension, DistributedPubSubMediator}
import akka.testkit.TestProbe

import TestSubscriber._
class TestSubscriber(args: Args) extends PdAkkaActor {
  import DistributedPubSubMediator.{ Subscribe, SubscribeAck }
  val mediator = DistributedPubSubExtension(context.system).mediator
  mediator ! Subscribe(args.topic, self)

  override def receive: Receive = {
    case SubscribeAck(Subscribe(_, None, `self`)) => // Dropped to not interfere with tests.
    case msg => args.probe.ref forward msg
  }
}

object TestSubscriber {
  case class Args(topic: String, probe: TestProbe) extends PdAkkaActor.Args(classOf[TestSubscriber])

  case object Initialized
}