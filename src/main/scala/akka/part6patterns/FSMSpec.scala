package akka.part6patterns

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, OneInstancePerTest, WordSpecLike}

import scala.concurrent.duration._

class FSMSpec extends TestKit(ActorSystem("FSMSpec"))
  with ImplicitSender with WordSpecLike with BeforeAndAfterAll
  with OneInstancePerTest /* otherwise test keep some state between tests and fail*/ {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import FSMSpecObject._

  def runTestSuite(props: Props): Unit ={
    "error when not initialized" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! RequestProduct("coke")
      expectMsg(VendingError("Machine not initialized"))
    }

    "report a product not available" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialize(Map("coke" ->10), Map("coke" -> 1))
      vendingMachine ! RequestProduct("pepsi")
      expectMsg(VendingError("ProductNotAvailable"))
    }

    "throw a timeout if I don't insert money" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialize(Map("coke" ->10), Map("coke" -> 1))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 1 dollars"))

      within(1.5 seconds){
        expectMsg(VendingError("RequestTimedOut"))
      }
    }

    "handle the reception of partial money" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialize(Map("coke" ->10), Map("coke" -> 3))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 3 dollars"))

      vendingMachine ! ReceiveMoney(1)
      expectMsg(Instruction("Please insert 2 dollars"))

      within(1.5 seconds){
        expectMsg(VendingError("RequestTimedOut"))
        expectMsg(GiveBackChange(1))
      }
    }

    "deliver the product if I insert all the money" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialize(Map("coke" ->10), Map("coke" -> 3))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 3 dollars"))
      vendingMachine ! ReceiveMoney(3)
      expectMsg(Deliver("coke"))
    }

    "give back change and be able to request money for a new product" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialize(Map("coke" ->10), Map("coke" -> 3))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 3 dollars"))

      vendingMachine ! ReceiveMoney(4)
      expectMsg(Deliver("coke"))
      expectMsg(GiveBackChange(1))

      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 3 dollars"))
    }
  }

  "A vending machine" should {
    runTestSuite(Props[VendingMachine])
  }

  "A vending machine FSM" should {
    runTestSuite(Props[VendingMachineFSM])
  }

}
