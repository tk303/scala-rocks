package akka.part4faulttolerance

import java.io.File

import akka.actor.SupervisorStrategy._
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.{Backoff, BackoffSupervisor}

import scala.io.Source
import scala.concurrent.duration._


object BackoffSupervisorPattern extends App {


  case object ReadFile

  class FileBasedPersistentActor extends Actor with ActorLogging {
    var dataSource: Source = null

    override def preStart(): Unit =
      log.info("Persistent actor is starting")

    override def postStop(): Unit =
      log.warning("Persistent actor has stopped")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit =
      log.warning("Persistent actor restarting")

    override def receive: Receive = {
      case ReadFile =>
        if(dataSource == null)
          dataSource = Source.fromFile((new File("src/main/resources/testfiles/impoefrtant.txt")))
        log.info("I've just read some IMPORTANT data: " + dataSource.getLines().toList)
    }
  }

  val system = ActorSystem("BackoffSupervisionDemo")
  //val simpleActor = system.actorOf(Props[FileBasedPErsistentActor], "simpleActor")
  //simpleActor ! ReadFile

  val simpleSupervisorProps = BackoffSupervisor.props(
    Backoff.onFailure(
      Props[FileBasedPersistentActor],
      "simpleBackoffActor",
      3 seconds,
      30 seconds,
      0.2
    )
  )

  //val simpleBackoffSupervisor = system.actorOf(simpleSupervisorProps, "simpleSupervisor")
  //simpleBackoffSupervisor ! ReadFile

  /*
      simpleSupervisor
        - creates a child called simpleBackoffActor (from pProbs of type FileBasedPersistentActor)
        (simpleSupervisor will forward all messages to the child)
        - supervision strategy is the default one (restarting on everything)
          * first attempt after 3 seconds
          * next attempt is 2x the previous attempt (3s, 6s, 12s,
          *
   */

  val stopSupervisorProps = BackoffSupervisor.props(
    Backoff.onStop(
      Props[FileBasedPersistentActor],
      "stopBackoffActor",
      3 seconds,
      30 seconds,
      0.2
    ).withSupervisorStrategy(
      OneForOneStrategy(){
        case _ => Stop
      }
    )
  )

  //val stopSupervisor = system.actorOf(stopSupervisorProps, "stopSupervisor")
  //stopSupervisor ! ReadFile

  ////////

  class EagerFBPActor extends FileBasedPersistentActor {
    override def preStart(): Unit = {
      log.info("Eager actor is starting")
      dataSource = Source.fromFile((new File("src/main/resources/testfiles/impoefrtant.txt")))
    }
  }

   // when actor throws ActorInitializationException then actor is stopped
  // _ : ActorInitializationException => STOP
  // so instead of:
  //val eagerActor = system.actorOf(Props[EagerFBPActor], "stopSupervisor")

  val repeatedSupervisorProps = BackoffSupervisor.props(
    Backoff.onStop(
      Props[EagerFBPActor],
      "eagerActor",
      1 second,
      30 seconds,
      0.1
    )
  )
  val repeatedSupervisor = system.actorOf(repeatedSupervisorProps, "eagerSupervisor")

  /*
      eagerSupervisor
        - child eagerActor
          - will die on start with the ActorInitializationException
          - trigger the supervision strategy in eagerSupervisor => STOP eagerActor
        - backoff will kick in after 1 second, 2s, 4, 8, 16.

   */

}
