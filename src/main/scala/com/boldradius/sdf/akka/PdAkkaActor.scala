package com.boldradius.sdf.akka

import akka.actor._

import PdAkkaActor._
trait PdAkkaActor extends Actor with ActorLogging {
  def createChild(actorArgs: Args, actorName: Option[String]): ActorRef = {
    createActor(context, actorArgs, actorName)
  }
}

object PdAkkaActor {
  abstract class Args(actorClass: Class[_ <: PdAkkaActor]) {
    val asProps: Props = Props(actorClass, this)
  }

  def createActor(refFactory: ActorRefFactory, actorArgs: Args, actorName: Option[String]): ActorRef = {
    createActor(refFactory, actorArgs.asProps, actorName)
  }
  def createActor(refFactory: ActorRefFactory, actorProps: Props, actorName: Option[String])
  : ActorRef = actorName match {
    case Some(name) => refFactory.actorOf(actorProps, name)
    case None => refFactory.actorOf(actorProps)
  }
}
