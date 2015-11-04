package com.boldradius.sdf.akka

import akka.actor._

import PdAkkaActor._

trait PdAkkaActor extends Actor with ActorLogging {
  def createChild(actorDef: Def) = {
    context.actorOf(actorDef.props)
  }
  def createChild(actorDef: Def, name: String) = {
    context.actorOf(actorDef.props, name)
  }
}

object PdAkkaActor {
  trait Def {
    val props: Props
  }
  abstract class DefNoParams[Actor <: PdAkkaActor](actorClass: Class[Actor]) extends Def {
    override val props = Props(actorClass)
  }
  abstract class DefWithParams[Actor <: PdAkkaActor](actorClass: Class[Actor]) extends Def {
    override val props = Props(actorClass, this)
  }
}
