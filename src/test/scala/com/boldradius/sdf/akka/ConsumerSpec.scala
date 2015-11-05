package com.boldradius.sdf.akka

import akka.actor._
import akka.actor.ActorDSL._
import akka.testkit._
import com.boldradius.sdf.akka.SessionLog.AppendRequest

class ConsumerSpec extends BaseAkkaSpec {

  "Sending a Request to Consumer" should {

    class TestConsumer(sessionLogs: Map[Long, TestProbe]) extends Consumer(Consumer.Args(system.deadLetters)) with TestPdAkkaActor {
      override def createTestChild(actorArgs: PdAkkaActor.Args, actorName: Option[String]) = actorArgs match {
        case args: SessionLog.Args if sessionLogs.contains(args.sessionId) =>
          assert(actorName.contains(args.sessionId.toString))
          sessionLogs(args.sessionId)
      }
    }
    def makeTestConsumer(sessionLogs: (Long, TestProbe)*) = new TestConsumer(sessionLogs.toMap)

    "result in creating a SessionLog" in {
      val sessionId = 1L
      val request = Request(sessionId, 0 /* timestamp */, "url", "referrer", "browser")
      val consumer = PdAkkaActor.createActor(system, Consumer.Args(system.deadLetters), Some("consumer-create-session-log"))

      consumer ! request
      TestProbe().expectActor(s"/user/consumer-create-session-log/${sessionId}")
    }

    "result in sending an AppendRequest to the SessionLog" in {
      val sessionId = 1L
      val sessionLog = TestProbe()
      val consumer = actor(makeTestConsumer(
        sessionId -> sessionLog))
      val request = Request(sessionId, 0 /* timestamp */, "url", "referrer", "browser")

      consumer ! request
      sessionLog.expectMsg(AppendRequest(request))
    }

    "result in sending two AppendRequests to the SessionLog when two Requests are received with the same session id" in {
      val sessionId = 1L
      val sessionLog = TestProbe()
      val consumer = actor(makeTestConsumer(
        sessionId -> sessionLog))
      val request = Request(sessionId, 0 /* timestamp */, "url", "referrer", "browser")

      consumer ! request
      consumer ! request
      sessionLog.expectMsg(AppendRequest(request))
      sessionLog.expectMsg(AppendRequest(request))
    }

    "result in creating two SessionLogs when two Requests are received wth different session ids" in {
      val sessionId1 = 1L
      val sessionId2 = 2L
      val sessionLog1 = TestProbe()
      val sessionLog2 = TestProbe()

      val consumer = actor(makeTestConsumer(
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
