package com.boldradius.sdf.akka

import akka.actor._
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

  override def receive: Receive = {
    case SessionData(requests) => {
      log.info(s"Aggregating session data for ${requests.size} requests.")
      requestsPerBrowser = mergeMaps(requestsPerBrowser, computeRequestsPerBrowser(requests))
      requestsByMinute = mergeMaps(requestsByMinute, computeRequestsByMinute(requests))
      requestsPerPage = mergeMaps(requestsPerPage, computeRequestsPerPage(requests))
      timePerUrl = mergeMaps(timePerUrl, computeTimePerUrl(requests))
      landingsPerPage = top(3, mergeMaps(landingsPerPage, computeLandingsPerPage(requests)))
      sinksPerPage = top(3, mergeMaps(sinksPerPage, computeSinksPerPage(requests)))
      usersPerBrowser = top(2, mergeMaps(usersPerBrowser, computeUsersPerBrowser(requests)))
      usersPerReferrer = top(2, mergeMaps(usersPerReferrer, computeUsersPerReferrer(requests)))
    }
  }

  private def computeRequestsPerBrowser(requests: Seq[Request]): Map[String, Int] =
    requests.groupBy(_.browser).mapValues(_.length)

  private def computeRequestsByMinute(requests: Seq[Request]): Map[Int, Int] =
    requests.groupBy { request =>
      val date = new java.util.Date(request.timestamp * 1000)
      (date.getHours * 60) + date.getMinutes
    }.mapValues(_.length)

  private def computeRequestsPerPage(requests: Seq[Request]): Map[String, Int] =
    requests.groupBy(_.url).mapValues(_.length)

  private def computeTimePerUrl(requests: Seq[Request]): Map[String, Int] =
    // NOTE: The last request is ignored, as we don't know how long the user is on that last page for.
    requests.zip(requests.tail).map {
      case (sourcePage, sinkPage) => (sourcePage.url, sinkPage.timestamp - sourcePage.timestamp)
    }.groupBy(_._1).mapValues(_.map(_._2).sum.toInt)

  private def computeLandingsPerPage(requests: Seq[Request]): Map[String, Int] =
    requests.headOption match {
      case Some(request) => Map(request.url -> 1)
      case None => Map()
    }

  private def computeSinksPerPage(requests: Seq[Request]): Map[String, Int] =
    requests.lastOption match {
      case Some(request) => Map(request.url -> 1)
      case None => Map()
    }

  private def computeUsersPerBrowser(requests: Seq[Request]): Map[String, Int] =
    requests.headOption match {
      case Some(request) => Map(request.browser -> 1)
      case None => Map()
    }

  private def computeUsersPerReferrer(requests: Seq[Request]): Map[String, Int] =
    requests.headOption match {
      case Some(request) => Map(request.referrer -> 1)
      case None => Map()
    }

  private def top[T](count: Int, map: Map[T, Int]): Map[T, Int] =
    map.toList.sortBy(_._2).reverse.take(count).toMap

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
  type TimePerUrl = Map[String, Int]  // [url, time_in_seconds]
  type LandingsPerPage = Map[String, Int]  // [page_name, num_landing_requests]
  type SinksPerPage = Map[String, Int]  // [page_num, num_sink_requests]
  type UsersPerBrowser = Map[String, Int]  // [browser, num_users]
  type UsersPerReferrer = Map[String, Int]  // [referrer, num_users]
}
