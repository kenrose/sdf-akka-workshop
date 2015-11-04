package com.boldradius.sdf.akka

import akka.actor._

import StatsAggregator._
class StatsAggregator(args: Args.type) extends PdAkkaActor {

  var requestsPerBrowser: RequestsPerBrowser = Map.empty[String, Int]
  var requestsByMinute: RequestsByMinute = Map.empty[Int, Int]
  var requestsPerPage: RequestsPerPage = Map.empty[String, Int]
  var timePerUrl: TimePerUrl = Map.empty[String, Int]
  var nonFinalRequestsPerUrl: NonFinalRequestsPerUrl = Map.empty[String, Int]
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
      nonFinalRequestsPerUrl = mergeMaps(nonFinalRequestsPerUrl, DataRequest.NonFinalRequestsPerUrl.compute(requests))
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
      sender() ! DataRequest.AverageVisitTimePerUrl.respond(timePerUrl, nonFinalRequestsPerUrl)

    case DataRequest.TopLandingPages.Request =>
      sender() ! DataRequest.TopLandingPages.respond(landingsPerPage)

    case DataRequest.TopSinkPages.Request =>
      sender() ! DataRequest.TopSinkPages.respond(sinksPerPage)

    case DataRequest.TopBrowsers.Request =>
      sender() ! DataRequest.TopBrowsers.respond(usersPerBrowser)

    case DataRequest.TopReferrers.Request =>
      sender() ! DataRequest.TopReferrers.respond(usersPerReferrer)
  }

  private def mergeMaps[K](a: Map[K, Int], b: Map[K, Int]): Map[K, Int] =
    a ++ b.map { case (browser, count) =>
      a.get(browser) match {
        case Some(v) => browser -> (count + v)
        case None => browser -> count
      }
    }
}

object StatsAggregator {
  case object Args extends PdAkkaActor.Args(classOf[StatsAggregator])

  case class SessionData(requests: Seq[Request])

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
