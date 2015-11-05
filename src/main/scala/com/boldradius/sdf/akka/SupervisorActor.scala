package com.boldradius.sdf.akka

import akka.actor._
import scala.util.control.NonFatal

import SupervisorActor._
class SupervisorActor(args: Args) extends PdAkkaActor with SettingsExtension {

  private val maxRestarts = settings.SUPERVISOR_RESTART_COUNT
  private val opsTeamEmail = settings.OPS_TEAM_EMAIL

  private val subordinate = createChild(args.subordinateArgs, Some(args.subordinateName))
  private val emailSender = createChild(EmailActor.Args, Some("emailer"))
  private var restartCount = 0

  override def receive: Receive = Actor.emptyBehavior

  override val supervisorStrategy: SupervisorStrategy = {
    val decider: SupervisorStrategy.Decider = {
      case NonFatal(ex) => {
        restartCount += 1
        if (restartCount > maxRestarts) {
          val message = s"Failed ${maxRestarts} times. Stopping."
          emailSender ! EmailActor.SendEmail(opsTeamEmail, message)
          SupervisorStrategy.Stop
        } else {
          SupervisorStrategy.Restart
        }
      }
    }
    OneForOneStrategy()(decider.orElse(super.supervisorStrategy.decider))
  }
}

object SupervisorActor {
  case class Args(subordinateArgs: PdAkkaActor.Args, subordinateName: String)
      extends PdAkkaActor.Args(classOf[SupervisorActor])
}
