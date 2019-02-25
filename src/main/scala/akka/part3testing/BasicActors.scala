package akka.part3testing

import akka.actor.{Actor, ActorSystem, Props}
import scala.util.Random

object BasicActors extends App {

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case message => sender() ! message
    }
  }

  class BlackHole extends Actor {
    override def receive: Receive = Actor.emptyBehavior
  }

  class LabTestActor extends Actor {
    val random = new Random()
    override def receive: Receive = {
      case "greeting" => if(random.nextBoolean()) sender ! "hi" else "hello"
      case "favouriteTech" =>
        sender() ! "Scala"
        sender() ! "Akka"
      case message : String => sender() ! message.toUpperCase()
    }
  }

  val system = ActorSystem("BasicActors")
  val actor = system.actorOf(Props[SimpleActor])
  actor ! "A message to remember"

}
