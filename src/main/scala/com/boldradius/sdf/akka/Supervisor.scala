package com.boldradius.sdf.akka

import akka.actor._
import scala.util.control.NonFatal

import Supervisor._
class Supervisor(args: Args) extends PdAkkaActor with SettingsExtension {
  private val opsTeamEmail = settings.OPS_TEAM_EMAIL

  private val subordinate = createChild(args.subordinateArgs, Some(args.subordinateName))
  private var restartCount = 0

  override def receive: Receive = {
    case GetSubordinate => sender() ! Subordinate(subordinate)
  }

  override val supervisorStrategy: SupervisorStrategy = {
    val decider: SupervisorStrategy.Decider = {
      case NonFatal(ex) => {
        restartCount += 1
        log.info(s"Caught exception.  restartCount=${restartCount}")
        if (restartCount > args.maxRestarts) {
          val message = s"Subordinate $subordinate failed $restartCount times. Stopping."
          args.emailSender ! EmailActor.SendEmail(opsTeamEmail, message)
          SupervisorStrategy.Stop
        } else {
          SupervisorStrategy.Restart
        }
      }
    }
    OneForOneStrategy()(decider.orElse(super.supervisorStrategy.decider))
  }
}

object Supervisor {
  case class Args(
    subordinateArgs: PdAkkaActor.Args,
    subordinateName: String,
    emailSender: ActorRef,
    maxRestarts: Int)
      extends PdAkkaActor.Args(classOf[Supervisor])

  case object GetSubordinate
  case class Subordinate(subordinate: ActorRef)
}
