package akka.part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.part2actors.ActorCapabilities.Person.LiveTheLife

object ActorCapabilities extends App {

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case "Hello" => sender() ! "Hello, there" // or condext.sender()
      case message: String => println(s"[simple actor: ${self}] I have received: ${message}")
      case number: Int => println(s"[simple actor] I have received a NUMBER: ${number}")
      case SpecialMessage(content) => println(s"[simple actor]  have received something SPECIAL: ${content}")
      case SendMessageToYourself(content) =>
        println("[simple actor] resending message to myself")
        self ! content
      case SayHiTo(ref) => ref ! "Hello"
      case WirelessPhoneMessage(content, ref) => ref forward content + " DISTORTED" // keeps the original sender
    }
  }

  val system = ActorSystem("actorCapabilitiesDemo")
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  simpleActor ! "hello, actor"

  // 1 - messages can be of any type
  // a) messages must be IMMUTABLE
  // b) messages must be SERIALIZABLE
  // => in practice use case classes and case objects
  simpleActor ! 42

  case class SpecialMessage(content: String)
  simpleActor ! SpecialMessage("some special content")


  // 2 - actors have information about their context and about themselves
  // context.self === 'this' in OOP // or just self

  case class SendMessageToYourself(content: String)
  simpleActor ! SendMessageToYourself("I am an actor and I'm proud of it")

  // 3 - actors can REPLY to messages
  val alice = system.actorOf(Props[SimpleActor], "alice")
  val bob = system.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)
  alice ! SayHiTo(bob)

  // 4 - dead letters
  alice ! "Hello" // there is no sender in such case (dead letters)

  // 5 - forwarding message, with the ORIGINAL sender
  // D -> A -> B

  case class WirelessPhoneMessage(content: String, ref: ActorRef)
  alice ! WirelessPhoneMessage("Let's play Wireless phone", bob)


  /**
    * Exercises
    *
    * 1. a Counter actor
    *   - Increment
    *   - Decrement
    *   - Print
    *
    * 2. a Bank account as an actor
    *  receives:
    *   - Deposit an amount
    *   - Withdraw an amount
    *   - Statement
    *     replies with:
    *       - Success
    *       - Failure
    */

  // DOMAIN object of the Counter
  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }

  class Counter extends Actor {
    import Counter._
    var counter : Int = 0
    override def receive: Receive = {
      case Increment => counter += 1
      case Decrement => counter -= 1
      case Print => println(s"[${self}] counter value: ${counter}")
    }
  }

  val counterActor = system.actorOf(Props[Counter], "counterActor")
  import Counter._
  (1 to 7).foreach( _ => counterActor ! Increment)
  (1 to 3).foreach( _ => counterActor ! Decrement)
  counterActor ! Print

  ////////////
  // domain object
  object BankAccount{
    case class Deposit(amount: BigDecimal)
    case class WithDraw(amount: BigDecimal)
    case class TransactionSuccess(message: String)
    case class TransactionFailure(reason: String)
    case object PrintBalance
  }

  class BankAccount extends Actor {
    import BankAccount._
    var balance : BigDecimal = 0
    override def receive: Receive = {
      case Deposit(amount) =>
        if(amount < 0 ) sender() ! TransactionFailure("invalid deposit amount")
        else {
          balance += amount
          println(s"[BankAccount] deposited: ${amount}")
          sender() ! TransactionSuccess(s"Successfully deposited: ${amount}")
        }
      case WithDraw(amount) =>
        if(amount < 0 ) sender() ! TransactionFailure("invalid withdraw amount")
        else if(amount > balance ) sender() ! TransactionFailure("insufficient funds to withdraw")
        else {
          balance -= amount
          sender() ! TransactionSuccess(s"Successfully withdrew: ${amount}")
        }
      case PrintBalance => println(s"[${self}] account balance: ${balance}")
    }
  }

  // if there are responses we can't send messages directly from the main program,
  // as we'd get the ' dead letters encountered' error

  object Person {
    case class LiveTheLife(account: ActorRef)
  }

  class Person extends Actor {
    import Person._
    import BankAccount._
    override def receive: Receive = {
      case LiveTheLife(account) =>
        println("[Person] living the life")
        account ! Deposit(BigDecimal(100))
        account ! WithDraw(BigDecimal(9000))
        account ! WithDraw(BigDecimal(500))
        account ! PrintBalance
      case message  => println(message.toString)
    }
  }

  val bankActor = system.actorOf(Props[BankAccount], "bankActor")

  import BankAccount._

  val person = system.actorOf(Props[Person], "person")

  person ! LiveTheLife(bankActor)

  // terminate the system
  //system.terminate()

}
