package com.boldradius.sdf.akka

import akka.actor._
import akka.actor.ActorDSL._
import akka.testkit._
import com.boldradius.sdf.akka.SessionLog.AppendRequest

import scala.collection.mutable

class ConsumerSpec extends BaseAkkaSpec {
  "1. Sending any message to Consumer" should {
    "a. result in logging the message" in {
      val consumer = PdAkkaActor.createActor(system, Consumer.Args, Some("consumer-1a"))
      EventFilter.info(pattern = ".*", occurrences = 1) intercept {
        consumer ! "Hello"
      }
    }
  }

  "2. Sending a Request to Consumer" should {

    class TestConsumer(sessionLogs: Map[Long, TestProbe]) extends Consumer(Consumer.Args) {
      val createdSessions = mutable.Set.empty[Long]
      override def createChild(actorArgs: PdAkkaActor.Args, actorName: Option[String]) = actorArgs match {
        case SessionLog.Args(sessionId) if sessionLogs.contains(sessionId) =>
          if (createdSessions.contains(sessionId)) {
            throw InvalidActorNameException(s"Tried to create the session twice: $sessionId")
          }
          createdSessions.add(sessionId)
          assert(actorName.contains(sessionId.toString))
          context.actorOf(sessionLogs(sessionId).asProps, actorName.get)
      }
    }
    def makeTestConsumer(sessionLogs: (Long, TestProbe)*) = new TestConsumer(sessionLogs.toMap)

    "a. result in creating a SessionLog" in {
      val sessionId = 1L
      val request = Request(sessionId, 0 /* timestamp */, "url", "referrer", "browser")
      val consumer = PdAkkaActor.createActor(system, Consumer.Args, Some("consumer-2a"))

      consumer ! request
      TestProbe().expectActor(s"/user/consumer-2a/${sessionId}")
    }

    "b. result in sending an AppendRequest to the SessionLog" in {
      val sessionId = 1L
      val sessionLog = TestProbe()
      val consumer = actor("consumer-2b")(makeTestConsumer(
        sessionId -> sessionLog))
      val request = Request(sessionId, 0 /* timestamp */, "url", "referrer", "browser")

      consumer ! request
      sessionLog.expectMsg(AppendRequest(request))
    }

    "c. result in sending two AppendRequests to the SessionLog when two Requests are received with the same session id" in {
      val sessionId = 1L
      val sessionLog = TestProbe()
      val consumer = actor("consumer-2c")(makeTestConsumer(
        sessionId -> sessionLog))
      val request = Request(sessionId, 0 /* timestamp */, "url", "referrer", "browser")

      consumer ! request
      consumer ! request
      sessionLog.expectMsg(AppendRequest(request))
      sessionLog.expectMsg(AppendRequest(request))
    }

    "d. result in creating two SessionLogs when two Requests are received wth different session ids" in {
      val sessionId1 = 1L
      val sessionId2 = 2L
      val sessionLog1 = TestProbe()
      val sessionLog2 = TestProbe()

      val consumer = actor("consumer-2d")(makeTestConsumer(
        sessionId1 -> sessionLog1,
        sessionId2 -> sessionLog2))
      val request1 = Request(sessionId1, 0 /* timestamp */, "url", "referrer", "browser")
      val request2 = Request(sessionId2, 0 /* timestamp */, "url", "referrer", "browser")

      consumer ! request1
      consumer ! request2
      sessionLog1.expectMsg(AppendRequest(request1))
      sessionLog2.expectMsg(AppendRequest(request2))
    }

  }
}
