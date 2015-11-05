package com.boldradius.sdf.akka

import akka.actor._
import akka.testkit._
import scala.concurrent.duration._

class EmailActorSpec extends BaseAkkaSpec {
  "Sending EmailActor a SendEmail message" should {
    "write a line to email.log" in {
      val emailActor = PdAkkaActor.createActor(system, EmailActor.Args, Some("email-actor"))
      val message = EmailActor.SendEmail("foo@bar.com", "Failure!")
      EventFilter.error(source = emailActor.path.toString, pattern = s".*foo.*", occurrences = 1) intercept {
        emailActor ! message
      }
      system.stop(emailActor)
    }
  }
}
