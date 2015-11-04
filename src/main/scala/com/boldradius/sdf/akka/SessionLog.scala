package com.boldradius.sdf.akka

import scala.collection.mutable.MutableList
import akka.actor._

class SessionLog(sessionId: Long) extends Actor with ActorLogging {
  log.info(s"SessionLog ${self} created for sessionId ${sessionId}")

  val requests = MutableList[Request]()

  import SessionLog._

  override def receive: Receive = {
    case AppendRequest(request) => {
      log.info(s"Appending request with URL ${request.url} to session ${sessionId}")
      requests += request
    }

    case msg => log.info(s"$self received message $msg")
  }
}

// WIP, totally changable
object SessionLog {
  def props(sessionId: Long): Props = Props(new SessionLog(sessionId))
  case class AppendRequest(request: Request)
}
