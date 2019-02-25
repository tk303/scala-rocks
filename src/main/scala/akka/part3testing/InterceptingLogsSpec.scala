package akka.part3testing

import akka.actor.{ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class InterceptingLogsSpec extends TestKit(ActorSystem("InterceptingLogsSpec", ConfigFactory.load().getConfig("interceptingLogMessages")))
  with ImplicitSender // gives us a 'testActor' that sends all the messages from the test
  with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import InterceptingLogs._

  val item = "Akka On Rocks"
  val creditCard = "1234-1234-1234-1234"
  val invalidCreditCard = "0000-0000-0000-0000"

  "A checkout flow" should {
    "correctly log the dispatch of an order" in {
      EventFilter.info(pattern = s"Order [0-9]+ for item $item has been dispatched", occurrences = 1) intercept { // can pass on a pattern to search messages for
        // our test code
        val checkoutRef = system.actorOf(Props[CheckoutActor])
        checkoutRef ! Checkout(item, creditCard)
      }
    }

    "freak out if the payment is denied" in {
      EventFilter[RuntimeException](occurrences =  1) intercept {
        val checkoutRef = system.actorOf(Props[CheckoutActor])
        checkoutRef ! Checkout(item, invalidCreditCard)
      }
    }

  }

}
