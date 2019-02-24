package akka.part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.part3testing.BasicActors.{BlackHole, LabTestActor, SimpleActor}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._

class BasicSpec extends TestKit(ActorSystem("BasicSpecs"))
  with ImplicitSender // gives us a 'testActor' that sends all the messages from the test
  with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "The thing being tested" should {
    "do this" in {
      // testing scenario
    }
  }

  "A simple actor" should {
    "send back the same message" in {
      // testing scenario
      val echoActor = system.actorOf(Props[SimpleActor])
      val message = "hello, test"
      echoActor ! message

      expectMsg(message) // akka.test.single-expect-default  -- to change default timeout of 3 seconds
    }
  }

  "A black hole actor" should {
    "send back same message" in {
      // testing scenario
      val blackHoleActor = system.actorOf(Props[BlackHole])
      val message = "hello, test"
      blackHoleActor ! message

      expectNoMessage(1 second) //
    }
  }

  // a message assertion
  "A lab test actor" should {
    val labTestActor = system.actorOf(Props[LabTestActor])

    "turn string into uppercase" in {
      labTestActor ! "I love Akka"
      val reply = expectMsgType[String]
      assert(reply == "I LOVE AKKA")
    }

    "reply to a greeting" in {
      labTestActor ! "greeting"
      expectMsgAnyOf("hi", "hello")
    }

    "reply with favourite tech" in {
      labTestActor ! "favouriteTech"
      expectMsgAllOf("Scala", "Akka")
    }

    "reply with favourite tech in a different way" in {
      labTestActor ! "favouriteTech"
      val messages = receiveN(2)
      // free to do more complicated assertions

    }

    "reply with favourite tech in a fancy way" in {
      labTestActor ! "favouriteTech"
      expectMsgPF() {
        case "Scala" => // only care that the PF is defined
        case "Akka" =>
      }

    }
  }


}
