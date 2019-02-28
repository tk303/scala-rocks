package akka.part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.{Config, ConfigFactory}

object Mailboxes extends App {

  val system = ActorSystem("Mailbox", ConfigFactory.load().getConfig("mailboxesDemo"))

  class SimpleActor extends Actor with ActorLogging {

    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  /** #1 - Custom priority mailbox
    *
    *  P0 -> most important
    *  P1
    *  P2
    *  P3 -> least important
    * */

  // step 1 - mailbox definition
  class SupportTicketPriorityMailbox(settings: ActorSystem.Settings, config: Config)
    extends UnboundedPriorityMailbox(
      PriorityGenerator {
        case message: String if message.startsWith("[P0]") => 0
        case message: String if message.startsWith("[P1]") => 1
        case message: String if message.startsWith("[P2]") => 2
        case message: String if message.startsWith("[P3]") => 3
        case _ => 4
      }
    ) {
  }

  // step2 - make it known in the config (application.conf)
  // step3 - attach the dispatcher to an actor
  val supportTicketLogger = system.actorOf(Props[SimpleActor].withDispatcher("support-ticket-dispatcher"))
  //supportTicketLogger ! "[P3] this thing would be nice to have"
  //supportTicketLogger ! "[P0] this needs to be solved NOW!"
  //supportTicketLogger ! "[P1] do this next"
  //supportTicketLogger ! "[P2] do this if you have time"

  /** #2 Control-aware mailbox
    * we will use UnboundedControlAwareMailbox **/

  // 2.1
  // step 1 - mark important messages as control messages
  case object ManagementTicket extends ControlMessage

  // step 2 - configure who gets the mailbox
  val controlAwareActor = system.actorOf(Props[SimpleActor].withMailbox("control-mailbox"))
//  controlAwareActor ! "[P3] this thing would be nice to have"
//  controlAwareActor ! "[P0] this needs to be solved NOW!"
//  controlAwareActor ! "[P1] do this next"
//  controlAwareActor ! "[P2] do this if you have time"
//  controlAwareActor ! ManagementTicket

  // 2.2 - using deployment config (altControlAwareActor configured in application.conf)
  val altControlAwareActor = system.actorOf(Props[SimpleActor], "altControlAwareActor")
  altControlAwareActor ! "[P3] this thing would be nice to have"
  altControlAwareActor ! "[P0] this needs to be solved NOW!"
  altControlAwareActor ! "[P1] do this next"
  altControlAwareActor ! "[P2] do this if you have time"
  altControlAwareActor ! ManagementTicket

  // -> result: ManagementTicket is received as a first message

}
