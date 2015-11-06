package com.boldradius.sdf.akka

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.boldradius.sdf.akka.RealTimeStatsAggregator.{DataResponse, DataRequest}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfter

import scala.collection.mutable.MutableList
import scala.concurrent.Await
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global

class RealTimeStatsAggregatorSpec extends BaseAkkaSpec {
  implicit val timeout =  1 second: Timeout

  val BaseRequest = Request(1, 0, "url", "referrer", "browser")

  "Sending a request to StatsAggregator" should {
    "not change anything" in {
      val statsAggregator = PdAkkaActor.createActor(system, RealTimeStatsAggregator.Args, None)
      statsAggregator ! BaseRequest

      (statsAggregator ? DataRequest).mapTo[DataResponse].map { response =>
        response.totalNumberOfSessions shouldBe 1
        response.sessionsPerBrowser shouldBe Map("browser" -> 1)
        response.sessionsPerURL shouldBe Map("url" -> 1)
      }
    }
  }
}
