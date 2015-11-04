package com.boldradius.sdf.akka

import scala.collection.mutable.MutableList
import akka.actor._

import SessionLog._
class SessionLog(args: Args) extends PdAkkaActor {
  log.info(s"SessionLog ${self} created for sessionId ${args.sessionId}")

  val requests = MutableList[Request]()

  override def receive: Receive = {
    case AppendRequest(request) => {
      log.info(s"Appending request with URL ${request.url} to session ${args.sessionId}")
      requests += request
    }

    case msg => log.info(s"$self received message $msg")
  }
}

object SessionLog {
  case class Args(sessionId: Long) extends PdAkkaActor.Args(classOf[SessionLog])
  case class AppendRequest(request: Request)
}
