package com.boldradius.sdf.akka

import akka.actor.Actor

case object Explode {
  def kaboom: Actor.Receive = {
    case Explode => throw new Exception("kaboom")
  }
}
