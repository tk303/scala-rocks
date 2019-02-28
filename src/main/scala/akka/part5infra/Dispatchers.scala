package akka.part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.routing.FromConfig
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

object Dispatchers extends App {

  class Counter extends Actor with ActorLogging {
    var count = 0

    override def receive: Receive = {
      case message =>
        count += 1
        log.info(s"[$count] $message")
    }
  }

  val system = ActorSystem("DispatchersDemo")//, ConfigFactory.load().getConfig("dispatchersDemo"))

  /** * Method #1 - programmatically */
  /** throughput = 30 means that message will be sent to a new actor after one has received 30 messages */
  //val actors = for(i <- 1 to 10) yield system.actorOf(Props[Counter].withDispatcher("my-dispatcher"), s"counter_$i")
  val r = new Random()
  /*for(i <- 1 to 1000) {
    actors(r.nextInt(10)) ! i // despite specifying the actor it will use the dispatcher
  }*/

  /** Method #2 - through configuration
  val rtjvmActor = system.actorOf(FromConfig.props(Props[Counter]), "rtjvm")
  for(i <- 1 to 1000) {
    rtjvmActor ! i
  }
   -- method 2 not working - was not working on the course video either */

  /** Dispatchers implement the ExecutionContext trait */
  class DBActor extends Actor with ActorLogging {
    /** Solution #0 (wrong!) -- using a context dispatcher will take up the resource from other actors which may get blocked */
    //implicit val executionContext: ExecutionContext = context.dispatcher

    /** Solution #1 - use a different dispatcher, for example one configured in our configuration */
    //implicit val executionContext: ExecutionContext = context.system.dispatchers.lookup("my-dispatcher")

    /** Solution #2 - use a router */
    implicit val executionContext: ExecutionContext = context.system.dispatchers.lookup("my-dispatcher")
    override def receive: Receive = {
      case message => Future {
        // wait on a resource
        Thread.sleep(5000)
        log.info(s"Success: $message")
      }
    }
  }

  val dbActor = system.actorOf(Props[DBActor], "dbActor")
  //dbActor ! "The meaning of life is 42"


  val nonBlockingActor = system.actorOf(Props[Counter])
  for(i <- 1 to 1000) {
    val message = s"important message $i"
    dbActor ! message
    nonBlockingActor ! message
  }
}
