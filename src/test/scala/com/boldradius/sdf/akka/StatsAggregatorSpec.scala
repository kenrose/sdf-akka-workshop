package com.boldradius.sdf.akka

import akka.pattern.ask
import akka.util.Timeout
import com.boldradius.sdf.akka.StatsAggregator.{DataRequest, SessionData}

import scala.collection.mutable.MutableList
import scala.concurrent.Await
import scala.concurrent.duration._

class StatsAggregatorSpec extends BaseAkkaSpec {
  implicit val timeout =  1 second: Timeout

  val BaseRequest = Request(1, 0, "url", "referrer", "browser")

  "Sending an empty SessionData to StatsAggregator" should {
    "not change anything" in {
      val statsAggregator = PdAkkaActor.createActor(system, StatsAggregator.Args, None)
      statsAggregator ! SessionData(Seq.empty)
    }
  }

  "Sending SessionData to StatsAggregator" should {

    "set requests per browser correctly" in {
      val statsAggregator = PdAkkaActor.createActor(system, StatsAggregator.Args, None)

      val browser1 = "b1"
      val browser2 = "b2"
      val browser3 = "b3"

      val requests = MutableList[Request]()

      requests += BaseRequest.copy(browser = browser1)
      requests += BaseRequest.copy(browser = browser1)
      requests += BaseRequest.copy(browser = browser2)
      statsAggregator ! SessionData(requests)

      val resp = Await.result(statsAggregator.ask(DataRequest.RequestsPerBrowser.Request).mapTo[DataRequest.RequestsPerBrowser.Response], 1 second)
      resp.response.get(browser1).get shouldBe 2
      resp.response.get(browser2).get shouldBe 1
      resp.response.get(browser3) shouldBe None

      
      requests.clear()
      val BaseRequest2 = BaseRequest.copy(sessionId = 2)
      requests += BaseRequest2.copy(browser = browser1)
      requests += BaseRequest2.copy(browser = browser3)
      statsAggregator ! SessionData(requests)

      val resp2 = Await.result(statsAggregator.ask(DataRequest.RequestsPerBrowser.Request).mapTo[DataRequest.RequestsPerBrowser.Response], 1 second)
      resp2.response.get(browser1).get shouldBe 3
      resp2.response.get(browser2).get shouldBe 1
      resp2.response.get(browser3).get shouldBe 1
    }

    "set busiest minute correctly" in {
      val statsAggregator = PdAkkaActor.createActor(system, StatsAggregator.Args, None)

      val minute1 = 1
      val timestamp1 = 1 // fix
      val minute2 = 100
      val timestamp2 = 2 // fix
      val minute3 = 1000
      val timestamp3 = 3 // fix

      val requests = MutableList[Request]()
      requests += BaseRequest.copy(timestamp = timestamp1)
      requests += BaseRequest.copy(timestamp = timestamp2)
      requests += BaseRequest.copy(timestamp = timestamp3)
      statsAggregator ! SessionData(requests)

      val resp = Await.result(statsAggregator.ask(DataRequest.BusiestMinute.Request).mapTo[DataRequest.BusiestMinute.Response], 1 second)
      resp.response shouldBe Map(timestamp3 -> minute3)

      requests.clear()
      val BaseRequest2 = BaseRequest.copy(sessionId = 2)
      requests += BaseRequest2.copy(timestamp = timestamp1)
      requests += BaseRequest2.copy(timestamp = timestamp2)
      statsAggregator ! SessionData(requests)

      val resp2 = Await.result(statsAggregator.ask(DataRequest.BusiestMinute.Request).mapTo[DataRequest.BusiestMinute.Response], 1 second)
      resp2.response shouldBe Map(timestamp2 -> minute2)
    }

    "set page visit distribution correctly" in {
      val statsAggregator = PdAkkaActor.createActor(system, StatsAggregator.Args, None)

      val page1 = "p1"
      val page2 = "p2"
      val page3 = "p3"

      val requests = MutableList[Request]()
      requests += BaseRequest.copy(url = page1)
      requests += BaseRequest.copy(url = page1)
      requests += BaseRequest.copy(url = page2)
      statsAggregator ! SessionData(requests)

      val resp = Await.result(statsAggregator.ask(DataRequest.PageVisitDistribution.Request).mapTo[DataRequest.PageVisitDistribution.Response], 1 second)
      /*
      resp.pageVisitDistribution.get(page1).get shouldBe (2/3)
      resp.pageVisitDistribution.get(page2).get shouldBe (1/3)
      resp.pageVisitDistribution.get(page3) shouldBe None
      */

      requests.clear()
      val BaseRequest2 = BaseRequest.copy(sessionId = 2)
      requests += BaseRequest2.copy(url = page1)
      requests += BaseRequest2.copy(url = page3)
      statsAggregator ! SessionData(requests)

      val resp2 = Await.result(statsAggregator.ask(DataRequest.PageVisitDistribution.Request).mapTo[DataRequest.PageVisitDistribution.Response], 1 second)
      // expect page1 to be 3/5
      // expect page2 to be 1/5
      // expect page3 to be 1/5
    }

    "set average visit time per page correctly" in {
      val statsAggregator = PdAkkaActor.createActor(system, StatsAggregator.Args, None)

      val page1 = "p1"
      val page2 = "p2"
      val page3 = "p3"

      val requests = MutableList[Request]()
      requests += BaseRequest.copy(url = page1, timestamp = 0)
      requests += BaseRequest.copy(url = page1, timestamp = 100)
      requests += BaseRequest.copy(url = page2, timestamp = 150)
      requests += BaseRequest.copy(url = page2, timestamp = 250)
      statsAggregator ! SessionData(requests)

      //val resp = Await.result(statsAggregator.ask(DataRequest.AverageVisitTimePerUrl.Request).mapTo[DataRequest.AverageVisitTimePerUrl.Response], 1 second)
      // expect page1 to be 75
      // expect page2 to be 100
      // expect page3 to be 0

      requests.clear()
      val BaseRequest2 = BaseRequest.copy(sessionId = 2)
      requests += BaseRequest2.copy(url = page1, timestamp = 0)
      requests += BaseRequest2.copy(url = page1, timestamp = 100)
      requests += BaseRequest2.copy(url = page2, timestamp = 150)
      requests += BaseRequest2.copy(url = page2, timestamp = 350)
      requests += BaseRequest2.copy(url = page3, timestamp = 800)
      statsAggregator ! SessionData(requests)

      //val resp2 = Await.result(statsAggregator.ask(DataRequest.AverageVisitTimePerUrl.Request).mapTo[DataRequest.AverageVisitTimePerUrl.Response], 1 second)
      // expect page1 to be 75
      // expect page2 to be 150
      // expect page3 to be 500
    }

    "set top 3 landing pages correctly" in {
      val statsAggregator = PdAkkaActor.createActor(system, StatsAggregator.Args, None)

      val page1 = "p1"
      val page2 = "p2"
      val page3 = "p3"
      val page4 = "p4"

      val requests = MutableList[Request]()

      requests += BaseRequest.copy(sessionId = 1, url = page1)
      statsAggregator ! SessionData(requests)

      requests.clear()
      requests += BaseRequest.copy(sessionId = 2, url = page1)
      statsAggregator ! SessionData(requests)

      requests.clear()
      requests += BaseRequest.copy(sessionId = 3, url = page2)
      statsAggregator ! SessionData(requests)

      val resp = Await.result(statsAggregator.ask(DataRequest.TopLandingPages.Request).mapTo[DataRequest.TopLandingPages.Response], 1 second)
      // expect page1 to be 2
      // expect page2 to be 1
      // expect page3 to be 0
      // expect page4 to be 0?

      requests.clear()
      requests += BaseRequest.copy(sessionId = 4, url = page1)
      statsAggregator ! SessionData(requests)

      requests.clear()
      requests += BaseRequest.copy(sessionId = 5, url = page3)
      statsAggregator ! SessionData(requests)

      val resp2 = Await.result(statsAggregator.ask(DataRequest.TopLandingPages.Request).mapTo[DataRequest.TopLandingPages.Response], 1 second)
      // expect page1 to be 3
      // expect page2 to be 1
      // expect page3 to be 1
    }
  }
}
