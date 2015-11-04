package com.boldradius.sdf.akka

import akka.actor._

import Consumer._
class Consumer(args: Args.type) extends PdAkkaActor {
  override def receive: Receive = {
    case req: Request => findOrCreateSessionLog(req.sessionId) ! SessionLog.AppendRequest(req)
    case msg => log.info(s"Consumer $self received message $msg")
  }

  protected def findOrCreateSessionLog(sessionId: Long): ActorRef = {
    context.child(sessionId.toString).getOrElse {
      createChild(SessionLog.Args(sessionId), Some(sessionId.toString))
    }
  }
}

object Consumer {
  case object Args extends PdAkkaActor.Args(classOf[Consumer])
}
