package akka.part6patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}

object StashDemo extends App {

  /**
      ResourceActor
        - open => it can receive read/write requests to teh resource
        - otherwise it will postpone all read/write requests until the state is open

        ResourceActor is closed
          - Open => switch to open state
          - Read, Write messages are POSTPONED

        ResourceActor is open
          - Read, Write are handled
          - Close => switch to closed state

      Example:
        [Open, Read, Read, Write] --> switch to open state, read the data, read again, write the data
        [Read, Open, Write] --> Stash = [Read], open => switch to open state, Mailbox: [Read,Write] - read and write are handled
   */

  case object Open
  case object Close
  case object Read
  case class Write(data: String)

  // step 1 - mixin the Stash trait
  class ResourceActor extends Actor with ActorLogging
    with Stash { // !! Stash trait overrides preRestart, so must be mixed in last, so super works well
    private var innerData : String = ""

    override def receive: Receive = closed

    def closed: Receive = {
      case Open =>
        log.info("Opening resource")
        // when switching message handle un stash all messages, hoping the next state can handle them
        unstashAll()
        context.become(open)
      case message =>
        log.info(s"Stashing $message because I can't handle it in the closed state")
        // step 3 - stash what you cannot handle
        stash()
    }

    def open: Receive = {
      case Read =>
        // do some actual computation
        log.info(s"I have read: $innerData")
      case Write(data) =>
        log.info(s"I am writing: $data")
        innerData = data
      case Close =>
        log.info("Closing resource")
        unstashAll()
        context.become(closed)
      case message =>
        log.info(s"Stashing $message because I can't handle it in the open state")
        stash()
    }
  }

    val system = ActorSystem("StashDemo")
    val resourceActor = system.actorOf(Props[ResourceActor], "resourceActor")

    resourceActor ! Read // stashed
    resourceActor ! Open // handled and pop Read
    resourceActor ! Open // stash
    resourceActor ! Write("I love stashing") // handled
    resourceActor ! Close // handled & switched to close, pop Open and switch back to open
    resourceActor ! Read // handled

}
