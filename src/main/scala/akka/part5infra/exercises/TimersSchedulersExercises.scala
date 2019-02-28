package akka.part5infra.exercises

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Timers}

import scala.concurrent.duration._

object TimersSchedulersExercises extends App {

  val system = ActorSystem("SchedulersTiersDemo")

  import system.dispatcher  // needs an implicit execution context

  /**
    * Exercise - implement a self-closing actor
    *
    *   - if the actor receives a message (anything), you have 1 second to end it another message
    *   - if the time window espires, the actor will stop itself
    *   - if you send another messge, the time window is reset
    */

  class SelfClosingActor extends Actor with ActorLogging {
    var schedule = createTimeoutWindow()

    def createTimeoutWindow(): Cancellable = {
      context.system.scheduler.scheduleOnce(1 second) {
        self ! "timeout"
      }
    }

    override def receive: Receive = {
      case "timeout" =>
        log.info("Stopping myself")
        context.stop(self)
      case message =>
        log.info(s"Received $message, staying alive")
        schedule.cancel()
        schedule = createTimeoutWindow()
    }
  }

  /*
  val selfClosingActor = system.actorOf(Props[SelfClosingActor], "selfClosingActor")
  system.scheduler.scheduleOnce(250 millis) {
    selfClosingActor ! "ping"
  }
  system.scheduler.scheduleOnce(2 seconds) {
    system.log.info("sending pong to self-closing actor")
    selfClosingActor ! "pong"
  }
  */

  /**
    * Timer -- sending messages to self
    */

  case object TimerKey
  case object Start
  case object Reminder
  case object Stop
  class TimerBasedHeartBeatActor extends Actor with ActorLogging with Timers {
    timers.startSingleTimer(TimerKey, Start, 500 millis)

    override def receive: Receive = {
      case Start =>
        log.info("bootstrapping")
        timers.startPeriodicTimer(TimerKey, Reminder, 1 second)
      case Reminder =>
        log.info("I am alive")
      case Stop =>
        log.warning("Stopping")
        timers.cancel(TimerKey)
        context.stop(self)
    }
  }

  val timerBasedHeartBeatActor = system.actorOf(Props[TimerBasedHeartBeatActor], "heartbeatActor")
  system.scheduler.scheduleOnce(5 seconds){
    timerBasedHeartBeatActor ! Stop
  }
}
