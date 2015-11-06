package com.boldradius.sdf.akka

import akka.actor._
import akka.testkit._
import com.boldradius.sdf.akka.SessionLog.SessionEnded
import scala.concurrent.duration._

class SessionLogSpec extends BaseAkkaSpec {
  "Sending a non-handled message to SessionLog" should {
    "not reset the receiveTimeout" in {
      val pubsubProbe = TestProbe()
      val testPubsub = PdAkkaActor.createActor(system, TestSubscriber.Args(Settings(system).SESSION_PUBSUB_TOPIC, pubsubProbe), None)

      val sessionLog = PdAkkaActor.createActor(system, SessionLog.Args(0), Some("session-log"))
      val sessionTimeout = Settings(system).REQUEST_SIMULATOR_SESSION_TIMEOUT

      // Send an unhandled message...
      sessionLog ! "unhandled message type!"

      // Then wait for half of the session timeout.
      Thread.sleep((sessionTimeout / 2).toMillis)

      // Then expect that we receive a Timeout around the time of the original timeout.
      pubsubProbe.within((sessionTimeout / 2) - (250 milliseconds), (sessionTimeout / 2) + (250 milliseconds)) {
        pubsubProbe.expectMsg(SessionEnded(Seq.empty))
      }
      system.stop(sessionLog)
    }

    "send a message to the stats actor after the timeout" in {
      val pubsubProbe = TestProbe()
      val testPubsub = PdAkkaActor.createActor(system, TestSubscriber.Args(Settings(system).SESSION_PUBSUB_TOPIC, pubsubProbe), None)

      val sessionLog = PdAkkaActor.createActor(system, SessionLog.Args(0), Some("session-log"))
      val sessionTimeout = Settings(system).REQUEST_SIMULATOR_SESSION_TIMEOUT

      // Then expect that we receive a Timeout around the time of the original timeout.
      pubsubProbe.within(sessionTimeout - (250 milliseconds), sessionTimeout + (250 milliseconds)) {
        pubsubProbe.expectMsg(SessionEnded(Seq.empty))
      }
      system.stop(sessionLog)
    }
  }
}
