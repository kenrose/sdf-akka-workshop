package com.boldradius.sdf.akka

import akka.actor._
import akka.testkit._

class SessionLogSpec extends BaseAkkaSpec {
  "Sending any message to Consumer" should {
    "result in logging the message" in {
      val consumer = system.actorOf(SessionLog.Def(66).props, "session-log")
      EventFilter.info(pattern = ".*received message.*", occurrences = 1) intercept {
        consumer ! "Hello"
      }
    }
  }
}
