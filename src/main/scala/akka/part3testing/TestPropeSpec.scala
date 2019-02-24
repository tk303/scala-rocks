package akka.part3testing

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class TestPropeSpec extends TestKit(ActorSystem("TestPropeSpec"))
  with ImplicitSender // gives us a 'testActor' that sends all the messages from the test
  with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import TestPropeActors._

  "A master actor" should {
    "register a slave" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")

      master ! Register(slave.ref)
      expectMsg(RegistrationAck)
    }

    "send the work to the slave actor" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")

      master ! Register(slave.ref)
      expectMsg(RegistrationAck)

      val workloadString = "I love Akka"
      master ! Work(workloadString)

      slave.expectMsg(SlaveWork(workloadString, testActor))

      // we can also mock the reply
      slave.reply(WorkCompletedCount(3, testActor))

      expectMsg(Report(3))
    }

    "aggregate data correctly" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")
      master ! Register(slave.ref)
      expectMsg(RegistrationAck)

      val workloadString = "I love Akka"
      master ! Work(workloadString)
      master ! Work(workloadString)

      // in the meantime I don't have a slave actor
      slave.receiveWhile(){
        case SlaveWork(`workloadString`, `testActor`) => slave.reply(WorkCompletedCount(3, testActor))
      }

      expectMsg(Report(3))
      expectMsg(Report(6))
    }
  }

}
