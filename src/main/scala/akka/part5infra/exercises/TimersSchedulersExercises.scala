package akka.part5infra.exercises

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props}
import akka.part5infra.TimersSchedulers.system

import scala.concurrent.duration._

object TimersSchedulers extends App {

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

  val selfClosingActor = system.actorOf(Props[SelfClosingActor], "selfClosingActor")
  system.scheduler.scheduleOnce(250 millis) {
    selfClosingActor ! "ping"
  }
}