package akka.part6patterns

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, FSM}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object FSMSpecObject {

  /** Vending machine */

  case class Initialize(inventory: Map[String, Int], prices: Map[String, Int])
  case class RequestProduct(product: String)

  case class Instruction(instruction: String) // message the VM (vending machine) will show on its' screen
  case class ReceiveMoney(amount: Int)
  case class Deliver(product: String)
  case class GiveBackChange(amount: Int)

  case class VendingError(reason: String)
  case object ReceiveMoneyTimeout

  class VendingMachine extends Actor with ActorLogging {
    implicit val executionContext: ExecutionContext = context.dispatcher
    override def receive: Receive = idle

    def idle: Receive = {
      case Initialize(inventory, prices) => context.become(operational(inventory, prices))
      case _ => sender() ! VendingError("Machine not initialized")
    }

    def operational(inventory: Map[String, Int], prices: Map[String, Int]): Receive = {
      case RequestProduct(product) => inventory.get(product) match {
        case None | Some(0) => sender() ! VendingError("ProductNotAvailable")
        case Some(_) =>
          val price = prices(product)
          sender() ! Instruction(s"Please insert $price dollars")
          context.become(waitForMoney(inventory, prices, product, 0, startReceiveMoneyTimeoutSchedule , sender()))
      }
    }

    def waitForMoney(inventory: Map[String, Int], prices: Map[String, Int], product: String,
                     money: Int, moneyTimeoutSchedule: Cancellable, requester: ActorRef): Receive = {
      case ReceiveMoneyTimeout =>
        requester ! VendingError("RequestTimedOut")
        if(money > 0) requester ! GiveBackChange(money)
        context.become(operational(inventory, prices))
      case ReceiveMoney(amount) =>
        moneyTimeoutSchedule.cancel()
        val price = prices(product)
        if(money + amount >= price){
          // user buys product
          requester ! Deliver(product)
          // deliver the change
          if(money + amount - price > 0) requester ! GiveBackChange(money + amount - price)
          // updating inventory
          val newStock = inventory(product) - 1
          val newInventory = inventory + (product -> newStock)
          context.become(operational(newInventory, prices))
        } else {
          val remainingMoney = price - money - amount
          requester ! Instruction(s"Please insert $remainingMoney dollars")
          context.become(waitForMoney(inventory, prices, product, // don't change
            money + amount, // user has inserted more money
          startReceiveMoneyTimeoutSchedule, // need to set the timeout again
            requester))
        }
    }

    def startReceiveMoneyTimeoutSchedule = context.system.scheduler.scheduleOnce(1 second){
      self ! ReceiveMoneyTimeout
    }
  }

  /****    Converting to FSM *****/

  // step 1 - define the states and the data of the actor
  trait VendingState
  case object Idle extends VendingState
  case object Operational extends VendingState
  case object WaitForMoney extends VendingState

  trait VendingData
  case object Uninitialized extends VendingData
  case class Initialized(inventory: Map[String, Int], prices: Map[String, Int]) extends VendingData
  case class WaitForMoneyData(inventory: Map[String, Int], prices: Map[String, Int], product: String,
                              money: Int, requester: ActorRef) extends VendingData

  class VendingMachineFSM extends FSM[VendingState, VendingData] {
    // we don't have a receive handler

    // instead we handle events and states
    // an EVENT(message, data)

    startWith(Idle, Uninitialized)

    // handling events:
    when(Idle) {
      case Event(Initialize(inventory, prices), Uninitialized) =>
        goto(Operational) using Initialized(inventory, prices) // equivalent of context.become(operational(inventory,prices))
      case _ =>
        sender() ! VendingError("Machine not initialized")
        stay() // stay in the same state
    }

    when(Operational){
      case Event(RequestProduct(product), Initialized(inventory, prices)) =>
        inventory.get(product) match {
          case None | Some(0) =>
            sender() ! VendingError("ProductNotAvailable")
            stay()
          case Some(_) =>
            val price = prices(product)
            sender() ! Instruction(s"Please insert $price dollars")
            goto(WaitForMoney) using WaitForMoneyData(inventory, prices, product, 0, sender())
        }
    }

    when(WaitForMoney, stateTimeout = 1 second) {
      case Event(StateTimeout, WaitForMoneyData(inventory, prices, product, money, requester)) =>
        requester ! VendingError("RequestTimedOut")
        if(money > 0) requester ! GiveBackChange(money)
        goto(Operational) using Initialized(inventory, prices)
      case Event(ReceiveMoney(amount), WaitForMoneyData(inventory, prices, product, money, requester)) =>
        val price = prices(product)
        if(money + amount >= price){
          // user buys product
          requester ! Deliver(product)
          // deliver the change
          if(money + amount - price > 0) requester ! GiveBackChange(money + amount - price)
          // updating inventory
          val newStock = inventory(product) - 1
          val newInventory = inventory + (product -> newStock)
          goto(Operational) using Initialized(newInventory, prices)
        } else {
          val remainingMoney = price - money - amount
          requester ! Instruction(s"Please insert $remainingMoney dollars")
          stay() using WaitForMoneyData(inventory, prices, product, money + amount, requester)
        }
    }

    whenUnhandled {
      case Event(_, _) =>
        sender() ! VendingError("CommandNotFound")
        stay()
    }

    onTransition{
      case stateA -> stateB => log.info(s"Transitioning from: $stateA to: $stateB")
    }

    initialize()
  }
}
