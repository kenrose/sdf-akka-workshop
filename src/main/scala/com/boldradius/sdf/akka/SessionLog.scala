package com.boldradius.sdf.akka

import scala.collection.mutable.MutableList
import scala.concurrent.duration.{Duration, SECONDS => Seconds}
import akka.actor._

class SessionLog(sessionId: Long, statsActor: ActorRef) extends Actor with ActorLogging {
  log.info(s"SessionLog ${self} created for sessionId ${sessionId}")

  val requests = MutableList[Request]()

  import SessionLog._
  private val sessionTimeout = Duration(
    context.system.settings.config.getDuration("request-simulator.session-timeout", Seconds),
    Seconds)
  context.setReceiveTimeout(sessionTimeout)

  override def receive: Receive = {
    case AppendRequest(request) => {
      log.info(s"Appending request with URL ${request.url} to session ${sessionId}")
      requests += request
    }

    case ReceiveTimeout => {
      statsActor ! "TODO REPLACE ME WITH RESULTS"
      context.setReceiveTimeout(Duration.Undefined)
      context.stop(self)
    }
  }
}

object SessionLog {
  def props(sessionId: Long, statsActor: ActorRef): Props =
    Props(new SessionLog(sessionId, statsActor))

  case class AppendRequest(request: Request)
}
