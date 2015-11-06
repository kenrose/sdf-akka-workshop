package com.boldradius.sdf.akka

import scala.collection.mutable.MutableList
import scala.concurrent.duration.Duration
import akka.actor._
import akka.contrib.pattern.{DistributedPubSubExtension, DistributedPubSubMediator}

import SessionLog._
class SessionLog(args: Args) extends PdAkkaActor with SettingsExtension {
  import DistributedPubSubMediator.Publish
  log.info(s"SessionLog ${self} created for sessionId ${args.sessionId}")

  val mediator = DistributedPubSubExtension(context.system).mediator

  val requests = MutableList[Request]()

  private val sessionTimeout = settings.REQUEST_SIMULATOR_SESSION_TIMEOUT
  context.setReceiveTimeout(sessionTimeout)

  override def receive: Receive = {
    case a @ AppendRequest(request) => {
      log.info(s"Appending request with URL ${request.url} to session ${args.sessionId}")
      requests += request
      mediator ! Publish(settings.SESSION_PUBSUB_TOPIC, a)
    }

    case ReceiveTimeout => {
      mediator ! Publish(settings.SESSION_PUBSUB_TOPIC, SessionEnded(requests))
      context.setReceiveTimeout(Duration.Undefined)
      context.stop(self)
    }
  }
}

object SessionLog {
  case class Args(sessionId: Long) extends PdAkkaActor.Args(classOf[SessionLog])
  case class AppendRequest(request: Request)
  case class SessionEnded(requests: Seq[Request])
}
