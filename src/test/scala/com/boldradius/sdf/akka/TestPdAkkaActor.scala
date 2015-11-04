package com.boldradius.sdf.akka

import akka.actor._
import akka.testkit._

import scala.collection.mutable

import PdAkkaActor._
trait TestPdAkkaActor extends PdAkkaActor {
  implicit def toTestProbeOps(probe: TestProbe): TestProbeOps = new TestProbeOps(probe)

  val createdChildren = mutable.Set.empty[String]
  abstract final override def createChild(actorArgs: Args, actorName: Option[String]): ActorRef = {
    actorName.foreach { name =>
      if (!createdChildren.contains(name)) createdChildren.add(name)
      else throw InvalidActorNameException(s"Tried to create the child twice: $name")
    }
    createActor(context, createTestChild(actorArgs, actorName).asProps, actorName)
  }
  def createTestChild(actorArgs: PdAkkaActor.Args, actorName: Option[String]): TestProbe
}
