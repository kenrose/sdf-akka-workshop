package com.boldradius.sdf.akka

import akka.actor._

class StatsAggregator() extends Actor with ActorLogging {
  import StatsAggregator._

  override def receive: Receive = {
  }
}

object StatsAggregator {
  def props: Props = Props(new StatsAggregator())
}
