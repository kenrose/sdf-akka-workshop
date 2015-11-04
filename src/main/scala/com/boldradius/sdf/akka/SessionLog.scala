package com.boldradius.sdf.akka

import akka.actor.{ActorLogging, Actor}

class SessionLog(sessionId: String) extends Actor with ActorLogging {
  log.info(s"SessionLog ${self} created for sessionId ${sessionId}")

  override def receive: Receive = {
    case msg => log.info(s"$self received message $msg")
  }
}

// WIP, totally changable
object SessionLog {
  case class AppendRequest(request: Request)
  case class RequestAppended(request: Request)
}
