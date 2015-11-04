package com.boldradius.sdf.akka

import akka.actor._
import sun.reflect.generics.reflectiveObjects.NotImplementedException
import scala.concurrent.duration._

class StatsAggregator() extends Actor with ActorLogging {
  import StatsAggregator._

  var requestsPerBrowser: RequestsPerBrowser = Map.empty[String, Int]
  var requestsByMinute: RequestsByMinute = Map.empty[Int, Int]
  var requestsPerPage: RequestsPerPage = Map.empty[String, Int]
  var timePerUrl: TimePerUrl = Map.empty[String, Int]
  var landingsPerPage: LandingsPerPage = Map.empty[String, Int]
  var sinksPerPage: SinksPerPage = Map.empty[String, Int]
  var usersPerBrowser: UsersPerBrowser = Map.empty[String, Int]
  var usersPerReferrer: UsersPerReferrer = Map.empty[String, Int]

  override def receive: Receive = handleSessionData.orElse(fetchData)

  private def handleSessionData: Receive = {
    case SessionData(requests) => {
      log.info(s"Aggregating session data for ${requests.size} requests.")
      requestsPerBrowser = mergeMaps(requestsPerBrowser, DataRequest.RequestsPerBrowser.compute(requests))
      requestsByMinute = mergeMaps(requestsByMinute, DataRequest.BusiestMinute.compute(requests))
      requestsPerPage = mergeMaps(requestsPerPage, DataRequest.PageVisitDistribution.compute(requests))
      timePerUrl = mergeMaps(timePerUrl, DataRequest.AverageVisitTimePerUrl.compute(requests))
      landingsPerPage = mergeMaps(landingsPerPage, DataRequest.TopLandingPages.compute(requests))
      sinksPerPage = mergeMaps(sinksPerPage, DataRequest.TopSinkPages.compute(requests))
      usersPerBrowser = mergeMaps(usersPerBrowser, DataRequest.TopBrowsers.compute(requests))
      usersPerReferrer = mergeMaps(usersPerReferrer, DataRequest.TopReferrers.compute(requests))
    }
  }

  private def fetchData: Receive = {
    case DataRequest.RequestsPerBrowser.Request =>
      sender() ! DataRequest.RequestsPerBrowser.respond(requestsPerBrowser)

    case DataRequest.BusiestMinute.Request =>
      sender() ! DataRequest.BusiestMinute.respond(requestsByMinute)

    case DataRequest.PageVisitDistribution.Request =>
      sender() ! DataRequest.PageVisitDistribution.respond(requestsPerPage)

    case DataRequest.AverageVisitTimePerUrl.Request =>
      sender() ! DataRequest.AverageVisitTimePerUrl.respond(timePerUrl, requestsPerPage)

    case DataRequest.TopLandingPages.Request =>
      sender() ! DataRequest.TopLandingPages.respond(landingsPerPage)

    case DataRequest.TopSinkPages.Request =>
      sender() ! DataRequest.TopSinkPages.respond(sinksPerPage)

    case DataRequest.TopBrowsers.Request =>
      sender() ! DataRequest.TopBrowsers.respond(usersPerBrowser)

    case DataRequest.TopReferrers.Request =>
      sender() ! DataRequest.TopReferrers.respond(usersPerReferrer)
  }

  private def mergeMaps[T](a: Map[T, Int], b: Map[T, Int]): Map[T, Int] =
    a ++ b.map { case (browser, count) =>
      a.get(browser) match {
        case Some(v) => browser -> (count + v)
        case None => browser -> count
      }
    }
}

object StatsAggregator {
  def props: Props = Props(new StatsAggregator())

  case class SessionData(requests: Seq[Request])

  type RequestsPerBrowser = Map[String, Int]  // [browser, num_requests]
  type RequestsByMinute = Map[Int, Int]  // [minute_starting_from_midnight, num_requests]
  type RequestsPerPage = Map[String, Int]  // [page_name, num_requests]
  type PageVisitDistribution = Map[String, Double]  // [page_name, num_requests]
  type TimePerUrl = Map[String, Int]  // [url, time_in_seconds]
  type AverageTimePerUrl = Map[String, Double]  // [url, time_in_seconds]
  type LandingsPerPage = Map[String, Int]  // [page_name, num_landing_requests]
  type SinksPerPage = Map[String, Int]  // [page_num, num_sink_requests]
  type UsersPerBrowser = Map[String, Int]  // [browser, num_users]
  type UsersPerReferrer = Map[String, Int]  // [referrer, num_users]

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
          (date.getHours * 60) + date.getMinutes
        }.mapValues(_.length)

      def respond(data: RequestsByMinute) = Response(data)
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
      def compute(requests: Seq[Request]) =
      // NOTE: The last request is ignored, as we don't know how long the user is on that last page for.
        requests.zip(requests.tail).map {
          case (sourcePage, sinkPage) => (sourcePage.url, sinkPage.timestamp - sourcePage.timestamp)
        }.groupBy(_._1).mapValues(_.map(_._2).sum.toInt)

      def respond(time: TimePerUrl, requests: RequestsPerPage) = {
        Response(time.map {
          case (url, totalTime) => url -> (totalTime.toDouble / requests(url))
        })
      }
    }

    case object TopLandingPages extends DataRequest[LandingsPerPage, LandingsPerPage] {
      def compute(requests: Seq[Request]) =
        Map(requests.head.url -> 1)

      def respond(data: LandingsPerPage) = Response(top(3, data))
    }

    case object TopSinkPages extends DataRequest[SinksPerPage, SinksPerPage] {
      def compute(requests: Seq[Request]) =
        Map(requests.last.url -> 1)

      def respond(data: SinksPerPage) = Response(top(3, data))
    }

    case object TopBrowsers extends DataRequest[UsersPerBrowser, UsersPerBrowser] {
      def compute(requests: Seq[Request]) =
        Map(requests.head.browser -> 1)

      def respond(data: UsersPerBrowser) = Response(top(2, data))
    }

    case object TopReferrers extends DataRequest[UsersPerReferrer, UsersPerReferrer] {
      def compute(requests: Seq[Request]) =
        requests.groupBy(_.referrer).mapValues(_.length)

      def respond(data: UsersPerReferrer) = Response(top(2, data))
    }
  }
}
