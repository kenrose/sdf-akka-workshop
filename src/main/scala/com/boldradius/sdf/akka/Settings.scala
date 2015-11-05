package com.boldradius.sdf.akka

import akka.actor.{Actor, ExtensionKey, Extension, ExtendedActorSystem}

import scala.concurrent.duration.{Duration, SECONDS => Seconds}


class Settings(system: ExtendedActorSystem) extends Extension {
  val REQUEST_SIMULATOR_SESSION_TIMEOUT =
    Duration(system.settings.config.getDuration("request-simulator.session-timeout", Seconds), Seconds)

  val SUPERVISOR_RESTART_COUNT =
    system.settings.config.getInt("request-simulator.supervisor-restart-count")

  val OPS_TEAM_EMAIL =
    system.settings.config.getString("request-simulator.ops-team-email")
}

object Settings extends ExtensionKey[Settings]

trait SettingsExtension {
  this: Actor =>

  val settings: Settings = Settings(context.system)
}
