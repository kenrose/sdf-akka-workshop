package com.boldradius.sdf.akka

import akka.actor._
import akka.testkit._

class ConsumerSpec extends BaseAkkaSpec {
  "Sending any message to Consumer" should {
    "result in logging the message" in {
      val consumer = system.actorOf(Consumer.Def.props, "consumer-log")
      EventFilter.info(pattern = ".*", occurrences = 1) intercept {
        consumer ! "Hello"
      }
    }
  }
}
