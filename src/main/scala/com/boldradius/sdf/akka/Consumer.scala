package com.boldradius.sdf.akka

import akka.actor._

import Consumer._
class Consumer(args: Args) extends PdAkkaActor {
  override def receive: Receive = {
    case req: Request => {
      findOrCreateSessionLog(req.sessionId) ! SessionLog.AppendRequest(req)
    }
  }

  protected def findOrCreateSessionLog(sessionId: Long): ActorRef = {
    context.child(sessionId.toString).getOrElse {
      createChild(SessionLog.Args(sessionId, args.statsActor), Some(sessionId.toString))
    }
  }
}

object Consumer {
  case class Args(statsActor: ActorRef) extends PdAkkaActor.Args(classOf[Consumer])
}
