package com.boldradius.sdf.akka

import akka.actor.ActorDSL._
import akka.actor.{ActorNotFound, ActorRef}
import akka.pattern.ask
import akka.testkit._
import akka.util.Timeout
import com.boldradius.sdf.akka.Supervisor.Subordinate

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

class SupervisorSpec extends BaseAkkaSpec {
  implicit val timeout: Timeout = 100.milliseconds

  val emailer = TestProbe()
  val supervisorArgs = Supervisor.Args(EmptyPdAkkaActor.Args, "supervised", emailer.ref, 2)

  class TestSupervisor(subordinate: TestProbe) extends Supervisor(supervisorArgs) with TestPdAkkaActor {
    override def createTestChild(actorArgs: PdAkkaActor.Args, actorName: Option[String]) = actorArgs match {
      case EmptyPdAkkaActor.Args =>
        assert(actorName.contains("supervised"))
        subordinate
    }
  }

  def getSubordinate(supervisor: ActorRef): ActorRef = {
    val res = (supervisor ? Supervisor.GetSubordinate).mapTo[Subordinate]
    Await.result(res, Duration.Inf).subordinate
  }

  "Creating a Supervisor" should {
    "result in creating a subordinate" in {
      val supervisor = PdAkkaActor.createActor(system, supervisorArgs, Some("supervisor-1"))

      TestProbe().expectActor(s"/user/supervisor-1/supervised")
      system.stop(supervisor)
    }

    "result in creating the supplied subordinate" in {
      val subordinate = TestProbe()
      val supervisor = actor("supervisor")(new TestSupervisor(subordinate))
    }
  }

  "A supervisor" should {
    "respond correctly to a GetSubordinate message" in {
      val supervisor = PdAkkaActor.createActor(system, supervisorArgs, Some("supervisor-2"))
      val subordinate: ActorRef = getSubordinate(supervisor)

      assert(subordinate.path.toStringWithoutAddress == "/user/supervisor-2/supervised")
      system.stop(supervisor)
    }

    "restart its subordinate if it fails only a few times" in {
      val supervisor = PdAkkaActor.createActor(system, supervisorArgs, Some("supervisor-3"))
      val subordinate: ActorRef = getSubordinate(supervisor)

      subordinate ! Explode

      Thread.sleep(100)
      val selection = system.actorSelection("/user/supervisor-3/supervised")
      val result = Try(Await.result(selection.resolveOne(), timeout.duration))
      assert(result.isSuccess)

      emailer.expectNoMsg()

      system.stop(supervisor)
    }

    "stop its subordinate if it fails too many times" in {
      val supervisor = PdAkkaActor.createActor(system, supervisorArgs, Some("supervisor-3"))
      val subordinate: ActorRef = getSubordinate(supervisor)

      subordinate ! Explode
      subordinate ! Explode
      subordinate ! Explode

      Thread.sleep(100)
      val selection = system.actorSelection("/user/supervisor-3/supervised")
      val result = Try(Await.result(selection.resolveOne(), timeout.duration))
      result.isFailure shouldBe true
      an [ActorNotFound] should be thrownBy result.get

      emailer.expectMsgType[EmailActor.SendEmail]

      system.stop(supervisor)
    }
  }
}
