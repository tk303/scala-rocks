package akka.part3testing

import akka.actor.{Actor, ActorRef}

object TestProbeActors extends App {

  // scenario

  /*
      word counting actor hierarchy: master-slave

      send some work to the master
        - master sends the slave the piece of work
        - slave processes the work and replies to master
        - master aggregates the result
      master sends the total count to the original requester
   */

  case class Register(slaveRef: ActorRef)
  case object RegistrationAck
  case class Work(test: String)
  case class SlaveWork(text: String, originalRequester: ActorRef)
  case class WorkCompletedCount(count: Int, originalRequster: ActorRef)
  case class Report(totalCount: Int)

  class Master extends Actor {
    override def receive: Receive = {
      case Register(slaveRef) =>
        sender() ! RegistrationAck
        context.become(online(slaveRef, 0))
      case _ =>
    }

    def online(slaveRef: ActorRef, totalWordCount: Int): Receive = {
      case Work(text) => slaveRef ! SlaveWork(text, sender())
      case WorkCompletedCount(count, originalRequster) =>
        val newTotalCount = totalWordCount + count
        originalRequster ! Report(newTotalCount)
        context.become(online(slaveRef, newTotalCount))
    }

  }
}
