package com.boldradius.sdf.akka

import akka.actor._

class Consumer extends Actor with ActorLogging {
  override def receive: Receive = {
    case req: Request => findOrCreateSessionLog(req.sessionId) ! SessionLog.AppendRequest(req)
    case msg => log.info(s"Consumer $self received message $msg")
  }

  protected def findOrCreateSessionLog(sessionId: Long): ActorRef =
    context.child(sessionId.toString).getOrElse(createSessionLog(sessionId))

  protected def createSessionLog(sessionId: Long): ActorRef =
    // TODO: Replace the use of deadLetters here with a Stats actor.
    context.actorOf(SessionLog.props(sessionId, context.system.deadLetters), sessionId.toString)
}

object Consumer {
  def props: Props = Props(new Consumer)
}
