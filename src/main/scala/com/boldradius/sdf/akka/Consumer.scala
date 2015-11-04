package com.boldradius.sdf.akka

import akka.actor.{ActorRef, Props, ActorLogging, Actor}

class Consumer extends Actor with ActorLogging {
  override def receive: Receive = {
    case req: Request => findOrCreateActor(req.sessionId) ! req
    case msg => log.info(s"Consumer $self received message $msg")
  }

  def findOrCreateActor(sessionId: Long): ActorRef = context.child(sessionId.toString).orElse {
    context.actorOf(SessionLog.props, sessionId.toString)
  }
}

object Consumer {
  def props: Props = Props(new Consumer)
}
