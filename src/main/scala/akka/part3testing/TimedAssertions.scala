package akka.part3testing

import akka.actor.Actor
import scala.util.Random

object TimedAssertions {

  case class WorkResult(result: Int)

  class WorkerActor extends Actor {
    override def receive: Receive = {
      case "work" =>
        // simulate long computation
        Thread.sleep(500)
        sender() ! WorkResult(42)
      case "workSequence" =>
        val r = new Random()
        for(i <- 1 to 10){
          Thread.sleep(r.nextInt(50))
          sender() ! WorkResult(1)
        }
    }

  }
}
