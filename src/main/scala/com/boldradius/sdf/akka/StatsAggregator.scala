package com.boldradius.sdf.akka

import akka.actor._

import StatsAggregator._
import akka.contrib.pattern.{DistributedPubSubExtension, DistributedPubSubMediator}
import akka.persistence.{SnapshotOffer, PersistentActor}
import com.boldradius.sdf.akka.SessionLog.SessionEnded

class StatsAggregator(args: Args.type) extends PdAkkaActor with PersistentActor with SettingsExtension {

  import DistributedPubSubMediator.{ Subscribe, SubscribeAck }
  val mediator = DistributedPubSubExtension(context.system).mediator
  mediator ! Subscribe(settings.SESSION_PUBSUB_TOPIC, self)

  var state = State()

  private def fetchData: Receive = {
    case DataRequest.RequestsPerBrowser.Request =>
      sender() ! DataRequest.RequestsPerBrowser.respond(state.requestsPerBrowser)

    case DataRequest.BusiestMinute.Request =>
      sender() ! DataRequest.BusiestMinute.respond(state.requestsByMinute)

    case DataRequest.PageVisitDistribution.Request =>
      sender() ! DataRequest.PageVisitDistribution.respond(state.requestsPerPage)

    case DataRequest.AverageVisitTimePerUrl.Request =>
      sender() ! DataRequest.AverageVisitTimePerUrl.respond(state.timePerUrl, state.nonFinalRequestsPerUrl)

    case DataRequest.TopLandingPages.Request =>
      sender() ! DataRequest.TopLandingPages.respond(state.landingsPerPage)

    case DataRequest.TopSinkPages.Request =>
      sender() ! DataRequest.TopSinkPages.respond(state.sinksPerPage)

    case DataRequest.TopBrowsers.Request =>
      sender() ! DataRequest.TopBrowsers.respond(state.usersPerBrowser)

    case DataRequest.TopReferrers.Request =>
      sender() ! DataRequest.TopReferrers.respond(state.usersPerReferrer)
  }
  def updateState(requests: Seq[Request]): Unit =
    state = state.updated(requests)

  override def receiveRecover: Receive = {
    case evt: Seq[Request]                 => updateState(evt)
    case SnapshotOffer(_, snapshot: State) => state = snapshot
  }

  override def receiveCommand: Receive = handleSessionData.orElse(fetchData).orElse(subscribeAck)

  private def subscribeAck: Receive = {
    case SubscribeAck(Subscribe(topic, None, `self`)) => log.info(s"Subscribed to pubsub: ${topic}")
  }

  private def handleSessionData: Receive = {
    case SessionEnded(requests) => {
      log.info(s"Aggregating session data for ${requests.size} requests.")
      updateState(requests)

      saveSnapshot(state)
    }
  }

  override def persistenceId: String = "stats-aggregator"
}

object StatsAggregator {
  case object Args extends PdAkkaActor.Args(classOf[StatsAggregator])

  type RequestsPerBrowser = Map[String, Int]  // [browser, num_requests]
  type RequestsByMinute = Map[Int, Int]  // [minute_starting_from_midnight, num_requests]
  type RequestsPerPage = Map[String, Int]  // [page_name, num_requests]
  type PageVisitDistribution = Map[String, Double]  // [page_name, num_requests]
  type TimePerUrl = Map[String, Int]  // [url, sum of time]
  type NonFinalRequestsPerUrl = Map[String, Int]  // [url, number of non-final requests]
  type AverageTimePerUrl = Map[String, Double]  // [url, time_in_seconds]
  type LandingsPerPage = Map[String, Int]  // [page_name, num_landing_requests]
  type SinksPerPage = Map[String, Int]  // [page_num, num_sink_requests]
  type UsersPerBrowser = Map[String, Int]  // [browser, num_users]
  type UsersPerReferrer = Map[String, Int]  // [referrer, num_users]

  @SerialVersionUID(100L)
  case class State(requestsPerBrowser: RequestsPerBrowser = Map.empty[String, Int],
                   requestsByMinute: RequestsByMinute = Map.empty[Int, Int],
                   requestsPerPage: RequestsPerPage = Map.empty[String, Int],
                   timePerUrl: TimePerUrl = Map.empty[String, Int],
                   nonFinalRequestsPerUrl: NonFinalRequestsPerUrl = Map.empty[String, Int],
                   landingsPerPage: LandingsPerPage = Map.empty[String, Int],
                   sinksPerPage: SinksPerPage = Map.empty[String, Int],
                   usersPerBrowser: UsersPerBrowser = Map.empty[String, Int],
                   usersPerReferrer: UsersPerReferrer = Map.empty[String, Int]) extends Serializable {
    def updated(requests: Seq[Request]): State = {
      copy(
        requestsPerBrowser = mergeMaps(requestsPerBrowser, DataRequest.RequestsPerBrowser.compute(requests)),
        requestsByMinute = mergeMaps(requestsByMinute, DataRequest.BusiestMinute.compute(requests)),
        requestsPerPage = mergeMaps(requestsPerPage, DataRequest.PageVisitDistribution.compute(requests)),
        timePerUrl = mergeMaps(timePerUrl, DataRequest.AverageVisitTimePerUrl.compute(requests)),
        nonFinalRequestsPerUrl = mergeMaps(nonFinalRequestsPerUrl, DataRequest.NonFinalRequestsPerUrl.compute(requests)),
        landingsPerPage = mergeMaps(landingsPerPage, DataRequest.TopLandingPages.compute(requests)),
        sinksPerPage = mergeMaps(sinksPerPage, DataRequest.TopSinkPages.compute(requests)),
        usersPerBrowser = mergeMaps(usersPerBrowser, DataRequest.TopBrowsers.compute(requests)),
        usersPerReferrer = mergeMaps(usersPerReferrer, DataRequest.TopReferrers.compute(requests))
      )
    }
  }

  def mergeMaps[T](a: Map[T, Int], b: Map[T, Int]): Map[T, Int] = {
    a ++ b.map { case (browser, count) =>
      val v = a.get(browser).getOrElse(0)
      browser -> (count + v)
    }
  }

  sealed abstract class DataRequest[ComputeType <: Map[_, _], ResponseType] {
    case object Request
    def compute(requests: Seq[Request]): ComputeType

    case class Response(response: ResponseType)

    protected def top[T](count: Int, map: Map[T, Int]): Map[T, Int] =
      map.toList.sortBy(_._2).reverse.take(count).toMap
  }

  case object DataRequest {
    case object RequestsPerBrowser extends DataRequest[RequestsPerBrowser, RequestsPerBrowser] {
      def compute(requests: Seq[Request]) =
        requests.groupBy(_.browser).mapValues(_.length)

      def respond(data: RequestsPerBrowser) = Response(data)
    }

    case object BusiestMinute extends DataRequest[RequestsByMinute, RequestsByMinute] {
      def compute(requests: Seq[Request]) =
        requests.groupBy { request =>
          val date = new java.util.Date(request.timestamp * 1000)
          val minutesSinceStartOfDay = (date.getHours * 60) + date.getMinutes
          minutesSinceStartOfDay
        }.mapValues(_.length)

      def respond(data: RequestsByMinute) = Response(top(1, data))
    }

    case object PageVisitDistribution extends DataRequest[RequestsPerPage, PageVisitDistribution] {
      def compute(requests: Seq[Request]) =
        requests.groupBy(_.url).mapValues(_.length)
      def respond(data: RequestsPerPage) = {
        val sum = data.values.sum.toDouble
        Response(data.map {
          case (key, value) => (key, value / sum)
        })
      }
    }

    case object AverageVisitTimePerUrl extends DataRequest[TimePerUrl, AverageTimePerUrl] {
      def compute(requests: Seq[Request]) = requests match {
        case Nil => Map.empty[String, Int]

        // NOTE: The last request is ignored, as we don't know how long the user is on that last page for.
        case requests: Seq[Request] =>
          requests.zip(requests.tail).map {
            case (sourcePage, sinkPage) => (sourcePage.url, sinkPage.timestamp - sourcePage.timestamp)
          }.groupBy(_._1).mapValues(_.map(_._2).sum.toInt)
      }

      def respond(time: TimePerUrl, requests: NonFinalRequestsPerUrl) = {
        Response(time.map {
          case (url, totalTime) => url -> (totalTime.toDouble / requests(url))
        })
      }
    }

    case object NonFinalRequestsPerUrl extends DataRequest[NonFinalRequestsPerUrl, Unit] {
      def compute(requests: Seq[Request]) = PageVisitDistribution.compute(requests).mapValues(_ - 1)
    }

    case object TopLandingPages extends DataRequest[LandingsPerPage, LandingsPerPage] {
      def compute(requests: Seq[Request]) =
        requests.headOption.map(request => Map(request.url -> 1)).getOrElse(Map())

      def respond(data: LandingsPerPage) = Response(top(3, data))
    }

    case object TopSinkPages extends DataRequest[SinksPerPage, SinksPerPage] {
      def compute(requests: Seq[Request]) =
        requests.lastOption.map(request => Map(request.url -> 1)).getOrElse(Map())

      def respond(data: SinksPerPage) = Response(top(3, data))
    }

    case object TopBrowsers extends DataRequest[UsersPerBrowser, UsersPerBrowser] {
      def compute(requests: Seq[Request]) =
        requests.headOption.map(request => Map(request.browser -> 1)).getOrElse(Map())

      def respond(data: UsersPerBrowser) = Response(top(2, data))
    }

    case object TopReferrers extends DataRequest[UsersPerReferrer, UsersPerReferrer] {
      def compute(requests: Seq[Request]) =
        requests.groupBy(_.referrer).mapValues(_.length)

      def respond(data: UsersPerReferrer) = Response(top(2, data))
    }
  }
}
