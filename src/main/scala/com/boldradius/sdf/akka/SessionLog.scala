package com.boldradius.sdf.akka

import scala.collection.mutable.MutableList
import akka.actor._

import SessionLog._

class SessionLog(d: Def) extends PdAkkaActor {
  log.info(s"SessionLog ${self} created for sessionId ${d.sessionId}")

  val requests = MutableList[Request]()

  override def receive: Receive = {
    case AppendRequest(request) => {
      log.info(s"Appending request with URL ${request.url} to session ${d.sessionId}")
      requests += request
    }

    case msg => log.info(s"$self received message $msg")
  }
}

// WIP, totally changable
object SessionLog {
  case class Def(sessionId: Long) extends PdAkkaActor.DefWithParams(classOf[SessionLog])
  case class AppendRequest(request: Request)
  case class RequestAppended(request: Request)
}
