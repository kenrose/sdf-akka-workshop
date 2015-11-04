package com.boldradius.sdf.akka

import akka.actor._
import akka.testkit._

class SessionLogSpec extends BaseAkkaSpec {
  "Sending any message to SessionLog" should {
    "result in logging the message" in {
      val sessionLog = PdAkkaActor.createActor(system, SessionLog.Args(66), Some("session-log"))
      EventFilter.info(pattern = ".*received message.*", occurrences = 1) intercept {
        sessionLog ! "Hello"
      }
    }
  }
}
