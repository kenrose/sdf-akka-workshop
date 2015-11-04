package com.boldradius.sdf.akka

import akka.actor._

class SessionLog(sessionId: Long) extends Actor with ActorLogging {
  log.info(s"SessionLog ${self} created for sessionId ${sessionId}")

  override def receive: Receive = {
    case msg => log.info(s"$self received message $msg")
  }
}

// WIP, totally changable
object SessionLog {
  def props(sessionId: Long): Props = Props(new SessionLog(sessionId))
  case class AppendRequest(request: Request)
  case class RequestAppended(request: Request)
}
