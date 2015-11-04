package com.boldradius.sdf.akka

import akka.testkit._
import scala.concurrent.duration._

class SessionLogSpec extends BaseAkkaSpec {
  "Sending a non-handled message to SessionLog" should {
    "not reset the receiveTimeout" in {
      val statsProbe = TestProbe()
      val sessionLog = system.actorOf(SessionLog.props(0, statsProbe.ref), "session-log")
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
      val sessionLog = system.actorOf(SessionLog.props(0, statsProbe.ref), "session-log")
      val sessionTimeout = Settings(system).REQUEST_SIMULATOR_SESSION_TIMEOUT

      // Then expect that we receive a Timeout around the time of the original timeout.
      statsProbe.within(sessionTimeout - (250 milliseconds), sessionTimeout + (250 milliseconds)) {
        statsProbe.expectMsg(StatsAggregator.SessionData(Seq.empty))
      }
      system.stop(sessionLog)
    }
  }
}
