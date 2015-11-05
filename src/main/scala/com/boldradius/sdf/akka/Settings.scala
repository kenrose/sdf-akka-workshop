package com.boldradius.sdf.akka

import akka.actor.{Actor, ExtensionKey, Extension, ExtendedActorSystem}

import scala.concurrent.duration.{Duration, SECONDS => Seconds}


class Settings(system: ExtendedActorSystem) extends Extension {
  val REQUEST_SIMULATOR_SESSION_TIMEOUT =
    Duration(system.settings.config.getDuration("request-simulator.session-timeout", Seconds), Seconds)
  val SNAPSHOT_DIR =
    system.settings.config.getString("akka.persistence.snapshot-store.local.dir")
}

object Settings extends ExtensionKey[Settings]

trait SettingsExtension {
  this: Actor =>

  val settings: Settings = Settings(context.system)
}
