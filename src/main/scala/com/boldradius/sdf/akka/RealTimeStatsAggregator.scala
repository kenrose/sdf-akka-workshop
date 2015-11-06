package com.boldradius.sdf.akka

import akka.actor._
import com.boldradius.sdf.akka.SessionLog.AppendRequest

import RealTimeStatsAggregator._
class RealTimeStatsAggregator(args: Args.type) extends PdAkkaActor {
  var lastRequests = Map.empty[Long, Request]

  override def receive: Receive = {
    case AppendRequest(request) =>
      lastRequests = lastRequests + (request.sessionId -> request)
    case StatsAggregator.SessionData(requests) =>
      lastRequests = lastRequests - requests.head.sessionId
  }
}

object RealTimeStatsAggregator {
  case object Args extends PdAkkaActor.Args(classOf[RealTimeStatsAggregator])

  case object DataRequest
  case class DataResponse(totalNumberOfSessions: Int,
                           sessionsPerURL: Map[String, Int],
                           sessionsPerBrowser: Map[String, Int])
}
