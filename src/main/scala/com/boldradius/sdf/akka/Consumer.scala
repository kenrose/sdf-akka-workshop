package com.boldradius.sdf.akka

import akka.actor._

import Consumer._
class Consumer(args: Args.type) extends PdAkkaActor {
  override def receive: Receive = {
    case req: Request => {
      findOrCreateSessionLog(req.sessionId) ! SessionLog.AppendRequest(req)
    }
  }

  protected def findOrCreateSessionLog(sessionId: Long): ActorRef = {
    context.child(sessionId.toString).getOrElse {
      // TODO: Replace the use of deadLetters here with a Stats actor.
      createChild(SessionLog.Args(sessionId, context.system.deadLetters), Some(sessionId.toString))
    }
  }
}

object Consumer {
  case object Args extends PdAkkaActor.Args(classOf[Consumer])
}
