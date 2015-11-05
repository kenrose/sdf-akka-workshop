package com.boldradius.sdf.akka

import scala.collection.mutable.MutableList
import scala.concurrent.duration.Duration
import akka.actor._

import SessionLog._
class SessionLog(args: Args) extends PdAkkaActor with SettingsExtension {
  log.info(s"SessionLog ${self} created for sessionId ${args.sessionId}")

  val requests = MutableList[Request]()

  private val sessionTimeout = settings.REQUEST_SIMULATOR_SESSION_TIMEOUT
  context.setReceiveTimeout(sessionTimeout)

  override def receive: Receive = {
    case AppendRequest(request) => {
      log.info(s"Appending request with URL ${request.url} to session ${args.sessionId}")
      requests += request
    }

    case ReceiveTimeout => {
      args.statsActor ! StatsAggregator.SessionData(requests)
      context.setReceiveTimeout(Duration.Undefined)
      context.stop(self)
    }
  }
}

object SessionLog {
  case class Args(sessionId: Long, statsActor: ActorRef) extends PdAkkaActor.Args(classOf[SessionLog])
  case class AppendRequest(request: Request)
}
