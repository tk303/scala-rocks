package akka.part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props}

import scala.concurrent.duration._

object TimersSchedulers extends App {

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("SchedulersTiersDemo")
  val simpleActor = system.actorOf(Props[SimpleActor])

  system.log.info("Scheduling reminder for simpleActor")

  import system.dispatcher  // needs an implicit execution context

  system.scheduler.scheduleOnce(1 second){
    simpleActor ! "reminder"
  }

  val routine: Cancellable= system.scheduler.schedule(1 second, 2 seconds){
    simpleActor ! "heartbeat"
  }

  system.scheduler.scheduleOnce(12 seconds){
    routine.cancel()
  }


}
