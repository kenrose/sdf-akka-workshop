package com.boldradius.sdf.akka

import akka.testkit.TestProbe

class RequestSimulationExampleAppSpec extends BaseAkkaSpec {

  "Creating RequestSimulationExampleApp" should {
    "result in creating a top-level actor named 'consumer'" in {
      new RequestSimulationExampleApp(system)
      TestProbe().expectActor("/user/consumer")
    }
  }
}
