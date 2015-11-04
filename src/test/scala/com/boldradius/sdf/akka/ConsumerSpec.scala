package com.boldradius.sdf.akka

import akka.actor.ActorDSL._
import akka.testkit._
import com.boldradius.sdf.akka.SessionLog.AppendRequest

class ConsumerSpec extends BaseAkkaSpec {
  "Sending any message to Consumer" should {
    "result in logging the message" in {
      val consumer = system.actorOf(Consumer.props, "consumer")
      EventFilter.info(pattern = ".*", occurrences = 1) intercept {
        consumer ! "Hello"
      }

      system.stop(consumer)
    }
  }

  "Sending a Request to Consumer" should {

    "result in creating a SessionLog" in {
      val sessionId = 1
      val request = Request(sessionId /* sessionId */, 0 /* timestamp */, "url", "referrer", "browser")
      val consumer = system.actorOf(Consumer.props, "consumer")

      consumer ! request
      TestProbe().expectActor(s"/user/consumer/${sessionId}")

      system.stop(consumer)
    }

    "result in sending an AppendRequest to the SessionLog" in {
      val sessionLog = TestProbe()
      val consumer = actor("consumer")(new Consumer {
        override def createSessionLog(sessionId: Long) = sessionLog.ref
      })
      val request = Request(1 /* sessionId */, 0 /* timestamp */, "url", "referrer", "browser")

      consumer ! request
      sessionLog.expectMsg(AppendRequest(request))

      system.stop(consumer)
    }

    "result in sending two AppendRequests to the SessionLog when two Requests are received with the same session id" in {
      val sessionLog = TestProbe()
      val consumer = actor("consumer")(new Consumer {
        override def createSessionLog(sessionId: Long) = sessionLog.ref
      })
      val request = Request(1 /* sessionId */, 0 /* timestamp */, "url", "referrer", "browser")

      consumer ! request
      consumer ! request
      sessionLog.expectMsg(AppendRequest(request))
      sessionLog.expectMsg(AppendRequest(request))

      system.stop(consumer)
    }

    "result in creating two SessionLogs when two Requests are received wth different session ids" in {
      val sessionLog1 = TestProbe()
      val sessionLog2 = TestProbe()
      val unusedSessionLog = TestProbe()
      val sessionId1 = 1
      val sessionId2 = 2

      val consumer = actor("consumer")(new Consumer {
        override def createSessionLog(sessionId: Long) = {
          println(s"createSessionLog called with $sessionId")
          if (sessionId == sessionId1) sessionLog1.ref
          else if (sessionId == sessionId2) sessionLog2.ref
          else unusedSessionLog.ref
        }
      })
      val request1 = Request(sessionId1 /* sessionId */, 0 /* timestamp */, "url", "referrer", "browser")
      val request2 = Request(sessionId2 /* sessionId */, 0 /* timestamp */, "url", "referrer", "browser")

      consumer ! request1
      consumer ! request2
      sessionLog1.expectMsg(AppendRequest(request1))
      sessionLog2.expectMsg(AppendRequest(request2))

      system.stop(consumer)
    }

  }
}
