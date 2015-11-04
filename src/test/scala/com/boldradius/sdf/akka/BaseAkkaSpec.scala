/**
 * Copyright © 2014, 2015 Typesafe, Inc. All rights reserved. [http://www.typesafe.com]
 */

package com.boldradius.sdf.akka

import akka.actor._
import akka.testkit._
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration.{DurationInt, FiniteDuration}

abstract class BaseAkkaSpec extends BaseSpec with BeforeAndAfterAll {

  implicit def toTestProbeOps(probe: TestProbe): TestProbeOps = new TestProbeOps(probe)

  implicit val system = ActorSystem()
  system.eventStream.publish(TestEvent.Mute(EventFilter.debug()))
  system.eventStream.publish(TestEvent.Mute(EventFilter.info()))
  system.eventStream.publish(TestEvent.Mute(EventFilter.warning()))
  system.eventStream.publish(TestEvent.Mute(EventFilter.error()))

  override protected def afterAll(): Unit = {
    system.shutdown()
    system.awaitTermination()
  }
}

class TestProbeOps(probe: TestProbe) {
  def asProps: Props = Props(classOf[TestProbeProxy], probe)

  def expectActor(path: String, max: FiniteDuration = probe.remaining): ActorRef = {
    probe.within(max) {
      var actor = null: ActorRef
      probe.awaitAssert {
        (probe.system actorSelection path).tell(Identify(path), probe.ref)
        probe.expectMsgPF(100 milliseconds) {
          case ActorIdentity(`path`, Some(ref)) => actor = ref
        }
      }
      actor
    }
  }
}

class TestProbeProxy(probe: TestProbe) extends Actor {
  override def receive: Receive = {
    case msg => probe.ref forward msg
  }
}
