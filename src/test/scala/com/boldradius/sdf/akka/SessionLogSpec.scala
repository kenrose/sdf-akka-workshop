package com.boldradius.sdf.akka

import akka.testkit._

class SessionLogSpec extends BaseAkkaSpec {
  "Sending any message to Consumer" should {
    "result in logging the message" in {
      val consumer = system.actorOf(Consumer.props, "consumer-log")
      EventFilter.info(pattern = ".*", occurrences = 1) intercept {
        consumer ! "Hello"
      }
    }
  }
}
