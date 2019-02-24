package akka.part3testing

import akka.actor.{ActorSystem, Props}
import akka.part3testing.TestPropeActors.{Master, Register, RegistrationAck}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._

class TimedAssertionsSpec extends TestKit(ActorSystem("TestPropeSpec", ConfigFactory.load().getConfig("specialTimedAssertionsConfig")))
  with ImplicitSender // gives us a 'testActor' that sends all the messages from the test
  with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import TimedAssertions._

  "A worker actor" should {
    val worker = system.actorOf(Props[WorkerActor])

    "reply with the meaning of life in a timely manner" in {
      within(500 millis, 1 second){
        worker ! "work"
        expectMsg(WorkResult(42))
      }
    }

    "reply with valid work at a reasonable cadence" in {
      within(1 second){
        worker ! "workSequence"

        val results: Seq[Int] = receiveWhile[Int](max = 2 seconds, idle = 500 millis, messages = 10){
          case WorkResult(result) => result
        }

        assert(results.sum > 5)
      }
    }

    "reply to a test probe in a timely manner" in {
      within(1 second) {
        val probe = TestProbe()
        probe.send(worker, "work")

        // probes have their own timeout (apart from being in the within block)
        probe.expectMsg(WorkResult(42))
      }
    }

  }

}
