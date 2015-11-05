package com.boldradius.sdf.akka

import EmailActor._

class EmailActor(args: Args.type) extends PdAkkaActor {
  override def receive: Receive = {
    case req => {
      log.error(s"REQUEST: $req")
    }
  }
}

object EmailActor {
  case object Args extends PdAkkaActor.Args(classOf[EmailActor])
}