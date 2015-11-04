package com.boldradius.sdf.akka

import akka.actor._

class StatsAggregator() extends Actor with ActorLogging {
  import StatsAggregator._

  override def receive: Receive = {
    case x => log.info(s"StatsAggregator got: $x")
  }
}

object StatsAggregator {
  def props: Props = Props(new StatsAggregator())

  case class SessionData(requests: Seq[Request])
}
