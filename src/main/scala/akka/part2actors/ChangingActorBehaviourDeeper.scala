package akka.part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChangingActorBehaviourDeeper extends App {

  object FussyKid {
    case object KidAccept
    case object KidReject
    val HAPPY = "happy"
    val SAD = "sad"
  }

  object Mom {
    case class MomStart(kidRef: ActorRef)
    case class Food(food: String)
    case class Ask(message: String)
    val VEGETABLE = "veggies"
    val CHOCOLATE = "chocolate"
  }

  class Mom extends Actor {
    import FussyKid._
    import Mom._
    override def receive: Receive = {
      case MomStart(kidRef) =>
        // test our interaction
        kidRef ! Food(VEGETABLE)
        kidRef ! Food(VEGETABLE)
        kidRef ! Food(CHOCOLATE)
        kidRef ! Food(CHOCOLATE)
        kidRef ! Ask("do you want to play?")
      case KidAccept => println("Yay, my kid is happy!")
      case KidReject => println("My kid is sad, but healthy")
    }
  }
  import Mom._

  // context.become - has two parameters, with the second having a default value of true
  // context.become( <handler> , discardOld = true)
  // by default when calling .become, the old handler is immediately replaced with the new handler
  // when we use discardOld = false -- we add the new behaviour to the stack

  class StatelessFussyKid extends Actor {
    import FussyKid._
    import Mom._

    override def receive: Receive = happyReceive

    def happyReceive: Receive = { // Receive was just a partial function
      case Food(VEGETABLE) => context.become(sadReceive, false)
      case Food(CHOCOLATE) =>
      case Ask(_) => sender() ! KidAccept
    }

    def sadReceive: Receive = { // Receive was just a partial function
      case Food(VEGETABLE) => context.become(sadReceive, false) // become more sad
      case Food(CHOCOLATE) => context.unbecome()
      case Ask(_) => sender() ! KidReject
    }
  }

  val system = ActorSystem("changingActorBehaviourDemo")
  val fussyKid = system.actorOf(Props[StatelessFussyKid])
  val mom = system.actorOf(Props[Mom])
  mom ! MomStart(fussyKid)

  /* The handler on top of the stack is used to handle messages, unbecome takes the current handler of the stack

    Food(Veg) -> stack.push(sadReceive)
    Food(Veg) -> stack.push(sadReceive)
    Food(chocolate) -> stack.push(happyReceive)
   */


}
