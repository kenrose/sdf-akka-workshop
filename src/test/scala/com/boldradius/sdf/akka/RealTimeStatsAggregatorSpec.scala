package com.boldradius.sdf.akka

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.boldradius.sdf.akka.RealTimeStatsAggregator.{DataResponse, DataRequest}
import com.boldradius.sdf.akka.SessionLog.AppendRequest
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfter

import scala.collection.mutable.MutableList
import scala.concurrent.Await
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global

class RealTimeStatsAggregatorSpec extends BaseAkkaSpec {
  implicit val timeout =  1 second: Timeout

  val BaseRequest = Request(1, 0, "url", "referrer", "browser")

  "Sending a request to RealTimeStatsAggregator" should {
    "properly aggregate zero sessions" in {
      val statsAggregator = PdAkkaActor.createActor(system, RealTimeStatsAggregator.Args, None)
      val response = Await.result((statsAggregator ? DataRequest).mapTo[DataResponse], Duration.Inf)
      response.totalNumberOfSessions shouldBe 0
      response.sessionsPerBrowser shouldBe Map.empty
      response.sessionsPerURL shouldBe Map.empty
    }
    "properly aggregate one session" in {
      val statsAggregator = PdAkkaActor.createActor(system, RealTimeStatsAggregator.Args, None)
      statsAggregator ! AppendRequest(BaseRequest)

      val response = Await.result((statsAggregator ? DataRequest).mapTo[DataResponse], Duration.Inf)
      response.totalNumberOfSessions shouldBe 1
      response.sessionsPerBrowser shouldBe Map("browser" -> 1)
      response.sessionsPerURL shouldBe Map("url" -> 1)
    }
    "properly aggregate two distinct sessions" in {
      val statsAggregator = PdAkkaActor.createActor(system, RealTimeStatsAggregator.Args, None)
      statsAggregator ! AppendRequest(BaseRequest)
      statsAggregator ! AppendRequest(BaseRequest.copy(sessionId = 2))

      val response = Await.result((statsAggregator ? DataRequest).mapTo[DataResponse], Duration.Inf)
      response.totalNumberOfSessions shouldBe 2
      response.sessionsPerBrowser shouldBe Map("browser" -> 2)
      response.sessionsPerURL shouldBe Map("url" -> 2)
    }
    "properly aggregate two requests from the same session" in {
      val statsAggregator = PdAkkaActor.createActor(system, RealTimeStatsAggregator.Args, None)
      statsAggregator ! AppendRequest(BaseRequest)
      statsAggregator ! AppendRequest(BaseRequest.copy(timestamp = 2))

      val response = Await.result((statsAggregator ? DataRequest).mapTo[DataResponse], Duration.Inf)
      response.totalNumberOfSessions shouldBe 1
      response.sessionsPerBrowser shouldBe Map("browser" -> 1)
      response.sessionsPerURL shouldBe Map("url" -> 1)
    }
    "properly aggregate two sessions with different browsers" in {
      val statsAggregator = PdAkkaActor.createActor(system, RealTimeStatsAggregator.Args, None)
      statsAggregator ! AppendRequest(BaseRequest)
      statsAggregator ! AppendRequest(BaseRequest.copy(sessionId = 2, browser = "browser2"))

      val response = Await.result((statsAggregator ? DataRequest).mapTo[DataResponse], Duration.Inf)
      response.totalNumberOfSessions shouldBe 2
      response.sessionsPerBrowser shouldBe Map("browser" -> 1, "browser2" -> 1)
      response.sessionsPerURL shouldBe Map("url" -> 2)
    }
    "properly aggregate two sessions with different urls" in {
      val statsAggregator = PdAkkaActor.createActor(system, RealTimeStatsAggregator.Args, None)
      statsAggregator ! AppendRequest(BaseRequest)
      statsAggregator ! AppendRequest(BaseRequest.copy(sessionId = 2, url = "url2"))

      val response = Await.result((statsAggregator ? DataRequest).mapTo[DataResponse], Duration.Inf)
      response.totalNumberOfSessions shouldBe 2
      response.sessionsPerBrowser shouldBe Map("browser" -> 2)
      response.sessionsPerURL shouldBe Map("url" -> 1, "url2" -> 1)
    }
  }
}
