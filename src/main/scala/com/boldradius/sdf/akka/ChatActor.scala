package com.boldradius.sdf.akka

import ChatActor._
import akka.actor.ReceiveTimeout

import scala.concurrent.duration.Duration

class ChatActor(args: Args.type) extends PdAkkaActor with SettingsExtension {

  private val sessionTimeout = settings.HELP_PAGE_CHAT_TIMEOUT
  context.setReceiveTimeout(sessionTimeout)

  override def receive: Receive = {
    case StartChat =>
      startChat
      context.setReceiveTimeout(Duration.Undefined)
    case NavigatedAway =>
      context.stop(self)
    case ReceiveTimeout =>
      self ! StartChat
  }

  private def startChat: Unit = {
    log.info(s"Chat started: To: ${sender()}")
  }
}

object ChatActor {
  case object Args extends PdAkkaActor.Args(classOf[ChatActor])

  case object NavigatedAway
  case object StartChat
}
