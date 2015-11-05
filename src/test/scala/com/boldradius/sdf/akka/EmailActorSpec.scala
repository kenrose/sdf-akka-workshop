package com.boldradius.sdf.akka

import akka.actor._
import akka.testkit._
import scala.concurrent.duration._

class EmailActorSpec extends BaseAkkaSpec {
  "Sending EmailActor a SendEmail message" should {
    "write a line to email.log" in {
      val statsProbe = TestProbe()
      val sessionLog = PdAkkaActor.createActor(system, EmailActor.Args, Some("email-actor"))
      val sessionTimeout = Settings(system).REQUEST_SIMULATOR_SESSION_TIMEOUT

      // Send an unhandled message...
      sessionLog ! "unhandled message type!"

      // Then wait for half of the session timeout.
      Thread.sleep((sessionTimeout / 2).toMillis)

      // Then expect that we receive a Timeout around the time of the original timeout.
      statsProbe.within((sessionTimeout / 2) - (250 milliseconds), (sessionTimeout / 2) + (250 milliseconds)) {
        statsProbe.expectMsg(StatsAggregator.SessionData(Seq.empty))
      }
      system.stop(sessionLog)
    }

    "send a message to the stats actor after the timeout" in {
      val statsProbe = TestProbe()
      val sessionLog = PdAkkaActor.createActor(system, SessionLog.Args(0, statsProbe.ref), Some("session-log"))
      val sessionTimeout = Settings(system).REQUEST_SIMULATOR_SESSION_TIMEOUT

      // Then expect that we receive a Timeout around the time of the original timeout.
      statsProbe.within(sessionTimeout - (250 milliseconds), sessionTimeout + (250 milliseconds)) {
        statsProbe.expectMsg(StatsAggregator.SessionData(Seq.empty))
      }
      system.stop(sessionLog)
    }
  }
}
