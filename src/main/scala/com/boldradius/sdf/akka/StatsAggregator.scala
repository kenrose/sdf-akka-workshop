package com.boldradius.sdf.akka

import akka.actor._

class StatsAggregator() extends Actor with ActorLogging {
  import StatsAggregator._

  var requestsPerBrowser = Map.empty[String, Int]

  override def receive: Receive = {
    case SessionData(requests) => {
      log.info(s"Aggregating session data for ${requests.size} requests.")
      requestsPerBrowser = mergeMaps(requestsPerBrowser, computeRequestsPerBrowser(requests))
    }
  }

  private def computeRequestsPerBrowser(requests: Seq[Request]): Map[String, Int] =
    requests.groupBy(_.browser).mapValues(_.length)

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
  type AverageTimePerUrl = Map[String, Int]  // [url, time_in_seconds]
  type LandingsPerPage = Map[String, Int]  // [page_name, num_landing_requests]
  type SinksPerPage = Map[String, Int]  // [page_num, num_sink_requests]
  type UsersPerBrowser = Map[String, Int]  // [browser, num_users]
  type UsersPerReferrer = Map[String, Int]  // [referrer, num_users]
}
