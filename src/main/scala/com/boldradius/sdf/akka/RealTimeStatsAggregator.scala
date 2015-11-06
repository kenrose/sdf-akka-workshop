package com.boldradius.sdf.akka

import akka.actor._
import akka.contrib.pattern.{DistributedPubSubExtension, DistributedPubSubMediator}
import com.boldradius.sdf.akka.SessionLog.{SessionEnded, AppendRequest}

import RealTimeStatsAggregator._
class RealTimeStatsAggregator(args: Args.type) extends PdAkkaActor with SettingsExtension {

  import DistributedPubSubMediator.{ Subscribe, SubscribeAck }
  val mediator = DistributedPubSubExtension(context.system).mediator
  mediator ! Subscribe(settings.SESSION_PUBSUB_TOPIC, self)

  var lastRequests = Map.empty[Long, Request]

  override def receive: Receive = receiveData.orElse(subscribeAck)

  private def receiveData: Receive = {
    case AppendRequest(request) =>
      lastRequests = lastRequests + (request.sessionId -> request)
    case SessionEnded(requests) =>
      lastRequests = lastRequests - requests.head.sessionId
    case DataRequest =>
      sender() ! DataResponse(
        lastRequests.size,
        lastRequests.groupBy(_._2.url).transform { (k, v) => v.size },
        lastRequests.groupBy(_._2.browser).transform { (k, v) => v.size }
      )
  }

  private def subscribeAck: Receive = {
    case SubscribeAck(Subscribe(topic, None, `self`)) => log.info(s"Subscribed to pubsub: ${topic}")
  }
}

object RealTimeStatsAggregator {
  case object Args extends PdAkkaActor.Args(classOf[RealTimeStatsAggregator])

  case object DataRequest
  case class DataResponse(totalNumberOfSessions: Int,
                           sessionsPerURL: Map[String, Int],
                           sessionsPerBrowser: Map[String, Int])
}
