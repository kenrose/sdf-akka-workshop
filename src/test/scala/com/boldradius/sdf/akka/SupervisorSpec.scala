package com.boldradius.sdf.akka

import akka.actor.ActorDSL._
import akka.pattern.ask
import akka.testkit._
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._

class SupervisorSpec extends BaseAkkaSpec {

  val emailer = TestProbe()
  val supervisorArgs = Supervisor.Args(EmptyPdAkkaActor.Args, "supervised", emailer.ref)
  class TestSupervisor(subordinate: TestProbe) extends Supervisor(supervisorArgs) with TestPdAkkaActor {
    override def createTestChild(actorArgs: PdAkkaActor.Args, actorName: Option[String]) = actorArgs match {
      case EmptyPdAkkaActor.Args =>
        assert(actorName.contains("supervised"))
        subordinate
    }
  }

  "Creating a Supervisor" should {
    "result in creating a subordinate" in {
      val supervisor = PdAkkaActor.createActor(system, supervisorArgs, Some("supervisor"))

      TestProbe().expectActor(s"/user/supervisor/supervised")
      system.stop(supervisor)
    }
  }

  "A supervisor" should {
    "respond correctly to a GetSubordinate message" in {
      val subordinate = TestProbe()
      val supervisor = actor("supervisor")(new TestSupervisor(subordinate))

      implicit val executionContext = system.dispatcher
      implicit val timeout: Timeout = 100.milliseconds
      val res = (supervisor ? Supervisor.GetSubordinate).mapTo[Supervisor.Subordinate]
      val subordinateRef = Await.result(res, Duration.Inf).subordinate

      assert(subordinateRef.path.toString == "/user/supervisor/supervised")
    }
  }

}
