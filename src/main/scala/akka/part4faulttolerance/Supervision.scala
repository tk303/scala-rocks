package akka.part4faulttolerance

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorLogging, AllForOneStrategy, OneForOneStrategy, Props, SupervisorStrategy}

object Supervision {

  case object Report

  class Supervisor extends Actor with ActorLogging{
    override val supervisorStrategy : SupervisorStrategy = OneForOneStrategy() {
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }

    override def receive: Receive = {
      case props: Props =>
        val childRef = context.actorOf(props)
        sender() ! childRef
    }

  }

  class NoDeathOnRestartSupervisor extends Supervisor {
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      // empty
    }
  }

  class AllForOneSupervisor extends Supervisor {
    override val supervisorStrategy = AllForOneStrategy() {
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }
  }

  class FussyWordCounter extends Actor with ActorLogging{
    var words = 0

    override def preStart(): Unit = log.info("FussyWordCounter started")
    override def postStop(): Unit = log.info("FussyWordCounter stopped")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit =
      log.info(s"FussyWordCounter restarting because of ${reason.getMessage} and error ${reason}")

    override def postRestart(reason: Throwable): Unit =
      log.info("FussyWordCounter restarted")

    override def receive: Receive = {
      case Report => sender() ! words
      case "" => throw new NullPointerException("Sentence empty")
      case sentence : String =>
        if(sentence.length > 20) throw new RuntimeException("Sentence is too big")
        else if(!Character.isUpperCase(sentence(0))) throw new IllegalArgumentException("Sentence must start with upper case")
        words += sentence.split(" ").length
      case _ => throw new Exception("Can only receive Strings")
    }
  }
}
