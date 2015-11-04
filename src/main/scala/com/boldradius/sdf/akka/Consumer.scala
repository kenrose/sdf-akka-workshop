package com.boldradius.sdf.akka

import akka.actor.{ActorLogging, Actor}

class Consumer extends Actor with ActorLogging {
  override def receive: Receive = {
    case msg => log.info(s"Consumer $self received message $msg")
  }
}
