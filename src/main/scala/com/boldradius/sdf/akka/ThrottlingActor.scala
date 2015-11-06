package com.boldradius.sdf.akka

import akka.actor._
import scala.concurrent.duration.Duration

class ThrottlingActor(args: ThrottlingActor.Args) extends PdAkkaActor with SettingsExtension {
  import ThrottlingActor._

  private val subordinate = createChild(args.subordinateArgs, None)
  private var previousRequests = Seq.empty[Long]

  override def receive: Receive = {
    case GetSubordinate => sender() ! Subordinate(subordinate)
    case msg: SessionLog.AppendRequest => throttle(msg)
  }

  def throttle(msg: Any): Unit = {
    val incomingTime = System.currentTimeMillis()
    if (previousRequests.size < args.numberOfRequests) {
      subordinate.forward(msg)
      previousRequests = previousRequests :+ incomingTime
    } else if ((incomingTime - previousRequests.head) > args.period.toMillis) {
      subordinate.forward(msg)
      previousRequests = previousRequests.dropWhile { _ < (incomingTime - args.period.toMillis) } :+ incomingTime
    } else {
      // drop this request on the floor
      log.warning(s"Dropped a request on the floor: $msg. Next opening for a request at ${previousRequests.head + args.period.toMillis}")
    }
  }
}

object ThrottlingActor {
  case class Args(
                   subordinateArgs: PdAkkaActor.Args,
                   numberOfRequests: Int,
                   period: Duration)
    extends PdAkkaActor.Args(classOf[ThrottlingActor])

  case object GetSubordinate
  case class Subordinate(subordinate: ActorRef)
}
