package akka.part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChangingActorBehaviourExercises extends App {

  /**
    * Exercise 1:
    *
    *   Recreate the coutner Actor with context.become and !with NO MUTABLE STATE
    */

  // DOMAIN object of the Counter
  object Counter {
    case class Increment(state: Int)
    case class Decrement(state: Int)
    case object Print
  }

  class Counter extends Actor {
    import Counter._
    override def receive: Receive = countReceive(0)

    def countReceive(currentCount: Int): Receive = {
      case Increment =>
        println(s"[${currentCount}] incrementing")
        context.become(countReceive(currentCount + 1))
      case Decrement =>
        println(s"[${currentCount}] decrementing")
        context.become(countReceive(currentCount - 1))
      case Print => println(s"[counter my current count is: ${currentCount}")
    }

  }

  val system = ActorSystem("changingActorBehaviourExercises")
  val counterActor = system.actorOf(Props[Counter], "counterActor")
  import Counter._
  (1 to 7).foreach( _ => counterActor ! Increment)
  (1 to 3).foreach( _ => counterActor ! Decrement)
  counterActor ! Print

  /**
    * Exercise 2:
    *
    *   simplified voting system
    */

  case class Vote(candidate: String)
  case object VoteStatusRequest
  case class VoteStatusReply(candidate: Option[String])

  class Citizen extends Actor {
    var candidate: Option[String] = None
    override def receive: Receive = {
      case Vote(candidate) => this.candidate = Some(candidate)
      case VoteStatusRequest => sender() ! VoteStatusReply(candidate)
    }
  }

  case class AggregateVotes(citizens: Set[ActorRef])

  class VoteAggregator extends Actor {
    var stillWaiting: Set[ActorRef] = Set.empty
    var currentStats: Map[String, Int] = Map.empty
    override def receive: Receive = {
      case AggregateVotes(citizens) =>
        stillWaiting = citizens
        citizens.foreach(citizenRef => citizenRef ! VoteStatusRequest)
      case VoteStatusReply(None) => // a citizen hasn't voted yet
        sender() ! VoteStatusRequest // this might end up in a n infinite loop (not in our case, but should be careful)
      case VoteStatusReply(Some(candidate)) =>
        val newStillWaiting = stillWaiting - sender()
        val currentVotesOfCandidate : Int = currentStats.getOrElse(candidate, 0)
        currentStats = currentStats + (candidate -> (currentVotesOfCandidate + 1))
        if(newStillWaiting.isEmpty){
          println(s"[aggregator] poll stats: $currentStats")
        } else {
          stillWaiting = newStillWaiting
        }
    }
  }

  val alice = system.actorOf(Props[Citizen])
  val bob = system.actorOf(Props[Citizen])
  val charlie = system.actorOf(Props[Citizen])
  val daniel = system.actorOf(Props[Citizen])

  val voteAggregator = system.actorOf(Props[VoteAggregator])

  voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel))

  alice ! Vote("Martin")
  bob ! Vote("Jonas")
  charlie ! Vote("Roland")
  daniel ! Vote("Roland")

}
