package com.boldradius.sdf.akka

import akka.actor._

class Consumer extends PdAkkaActor {
  override def receive: Receive = {
    case req: Request => findOrCreateSessionLog(req.sessionId) ! SessionLog.AppendRequest(req)
    case msg => log.info(s"Consumer $self received message $msg")
  }

  protected def findOrCreateSessionLog(sessionId: Long): ActorRef = {
    context.child(sessionId.toString).getOrElse {
      createChild(SessionLog.Def(sessionId), sessionId.toString)
    }
  }
}

object Consumer {
  case object Def extends PdAkkaActor.DefNoParams(classOf[Consumer])
}
