package akka.part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Terminated}
import akka.routing._
import com.typesafe.config.ConfigFactory

object Routers extends App {

  /**
    * #1 - manual router
    */
  class Master extends Actor {
    // step 1 - create routes

    private val slaves = for (i <- 1 to 5) yield {
      val slave = context.actorOf(Props[Slave], s"slave_$i")
      context.watch(slave)
      ActorRefRoutee(slave)
    }

    // step 2 - define router
    private var router = Router(RoundRobinRoutingLogic(), slaves)

    override def receive: Receive = {
      // step 3 - route the message
      case message => router.route(message, sender())

      // step 4 - handle the termination/lifecycle of the routees
      case Terminated(ref) =>
        router = router.removeRoutee(ref)
        val newSlave = context.actorOf(Props[Slave])
        context.watch(newSlave)
        router = router.addRoutee(newSlave)
    }
  }

  class Slave extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("RoutersDemo", ConfigFactory.load().getConfig("routersDemo"))
  val master = system.actorOf(Props[Master], "masterActor")

  /*
  for (i <- 1 to 10)
    master ! s"[$i] Hello from the world"
    */

  /** * Method #2 */

  /**
    *2.1 - a router actor with its own children (programmatically)
    * (aka POOL router)
    */

  /*
  val poolMaster = system.actorOf(RoundRobinPool(5).props(Props[Slave]), "simplePoolMaster")
  for (i <- 1 to 10)
    master ! s"[$i] Hello from the other side"
    */

  /**
    *  2.2 - through configuration
    */
  /*val poolMaster2 = system.actorOf(FromConfig.props(Props[Slave]), "poolMaster2")
  for (i <- 1 to 10)
    poolMaster2 ! s"[$i] G'day mate" */


  /** *  Method #3 - with actors created elsewhere */

  /**
    *  3.1 - programmatically
    */

  // ... in another part of my application I create my slaves
  val slaveList = (1 to 5).map(i => system.actorOf(Props[Slave], s"slave_$i")).toList

  // need their paths
  val slavePaths = slaveList.map(slaveRef => slaveRef.path.toString)

  /* val groupMaster = system.actorOf(RoundRobinGroup(slavePaths).props())
  for (i <- 1 to 10)
    groupMaster ! s"[$i] Hello from another universe" */

  /**
    *  3.2 - from configuration
    */
  val groupMaster2 = system.actorOf(FromConfig.props(), "groupMaster2")
  for (i <- 1 to 10)
    groupMaster2 ! s"[$i] Hello from last one standing"

  /**
    * Special messages
    */
  // broadcasted to every slave
  groupMaster2 ! Broadcast("hello, everyone")

  // other special messages:
  // PoisonPill and Kill are NOT routed
  // AddRoutee, Remove, Get - handled only by the routing actor
}
