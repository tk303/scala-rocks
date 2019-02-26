package akka.part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}

object ActorLifecycle extends App {

  val system = ActorSystem("LifecycleDemo")

  /**
    * 1 - starting

  object StartChild

  class LifecycleActor extends Actor with ActorLogging {
    override def preStart(): Unit = log.info("I am starting (preStart)")
    override def postStop(): Unit = log.info("I have stopped (posStop)")

    override def receive: Receive = {
      case StartChild =>
        context.actorOf(Props[LifecycleActor], "child")
    }
  }

  val parent = system.actorOf(Props[LifecycleActor], "parent")

  parent ! StartChild
  parent ! PoisonPill

    */

  /**
    * 2 - restart
    */

  object Fail
  object FailChild
  object CheckChild
  object Check

  class Parent extends Actor{
    val child = context.actorOf(Props[Child], "supervisedChild")

    override def receive: Receive = {
      case FailChild => child ! Fail
      case CheckChild => child ! Check
    }
  }

  class Child extends Actor with ActorLogging {
    override def preStart(): Unit = log.info("supervised child started")
    override def postStop(): Unit = log.info("supervised child stopped")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit =
      log.info(s"supervised actor restarting because of ${reason.getMessage}")

    override def postRestart(reason: Throwable): Unit =
      log.info("supervised actor restarted")

    override def receive: Receive = {
      case Fail =>
        log.warning("Child will fail now")
        throw new RuntimeException("I have failed")
      case Check =>
        log.info("alive and kicking")
    }
  }

  val supervisor = system.actorOf(Props[Parent], "supervisor")
  supervisor ! FailChild
  supervisor ! CheckChild

  // default supervision strategy: restart child when fail
}
