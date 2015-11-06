package com.boldradius.sdf.akka

import akka.actor._
import scala.concurrent.duration._

/**
 * Manages active sessions, and creates more as needed
 */
import RequestProducer._
class RequestProducer(args: Args) extends PdAkkaActor {

  import context.dispatcher

  // Interval used to check for active sessions
  val checkSessionInterval = 100 milliseconds

  // We begin by waiting for a Start signal to arrive
  def receive: Receive = stopped

  def stopped: Receive = {
    case ConsumerRegistration(consumer) =>
      context.watch(consumer)
      // Move to a different state to avoid sending to more than one target
      context.become(producing)

      // Kickstart the session checking process
      self ! CheckSessions(consumer)

    case CheckSessions(consumer) =>
      log.debug("CheckSessions received.")
  }

  def producing: Receive = {
    case CheckSessions(consumer) =>
      // Check if more sessions need to be created, and schedule the next check
      checkSessions(consumer)
      context.system.scheduler.scheduleOnce(checkSessionInterval, self, CheckSessions(consumer))

    case Stop =>
      log.debug("Stopping simulation")
      context.become(stopped)

    case Terminated(consumer) =>
      self ! Stop
  }


  def checkSessions(consumer: ActorRef) {

    // Check child actors, if not enough, create one more
    val activeSessions = context.children.size
    log.debug(s"Checking active sessions - found $activeSessions for a max of ${args.concurrentSessions} concurrent sessions")

    if(activeSessions < args.concurrentSessions) {
      log.debug("Creating a new session")
      createChild(SessionRequestEmitter.Args(consumer, args.sessionInterval), None)
    }
  }
}


object RequestProducer {
  case class Args(concurrentSessions:Int, sessionInterval: Option[FiniteDuration]) extends PdAkkaActor.Args(classOf[RequestProducer])

  // Messaging protocol for the RequestProducer
  case class ConsumerRegistration(consumer: ActorRef)
  case object Stop
  case class CheckSessions(target: ActorRef)
}

