package com.boldradius.sdf.akka

import akka.actor._

import scala.concurrent.duration.FiniteDuration

// Wraps around a session and emits requests to the target actor
import com.boldradius.sdf.akka.SessionRequestEmitter._
class SessionRequestEmitter(args: Args) extends PdAkkaActor {

  import context.dispatcher

  // Upon actor creation, build a new session
  val session = new Session

  // This actor should only live for a certain duration, then shut itself down
  context.system.scheduler.scheduleOnce(session.duration, self, PoisonPill)

  // Start the simulation
  self ! Click

  override def receive = {
    case Click =>
      // Send a request to the target actor
      val request = session.request
      args.target ! request

      // Schedule a Click message to myself after some time visiting this page
      val pageDuration = args.interval.getOrElse(Session.randomPageTime(request.url))
      context.system.scheduler.scheduleOnce(pageDuration, self, Click)
  }
}

object SessionRequestEmitter {
  case class Args(target: ActorRef, interval: Option[FiniteDuration]) extends PdAkkaActor.Args(classOf[SessionRequestEmitter])

  // Message protocol for the SessionActor
  case object Click
}
