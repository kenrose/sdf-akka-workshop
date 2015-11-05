package com.boldradius.sdf.akka

import EmailActor._

class EmailActor(args: Args.type) extends PdAkkaActor {
  override def receive: Receive = {
    case msg : SendEmail => {
      sendEmail(msg)
    }
  }

  private def sendEmail(msg: SendEmail): Unit = {
    log.error(s"To: ${msg.recipient}\n${msg.body}")
  }
}

object EmailActor {
  case object Args extends PdAkkaActor.Args(classOf[EmailActor])

  case class SendEmail(recipient: String, body: String)
}
