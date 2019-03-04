package akka.part6patterns

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class AskSpec extends TestKit(ActorSystem("AskSpec"))
  with ImplicitSender with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import AskSpecObject._

  "An authenticator" should {
    authenticatorTestSuite(Props[AuthManager])
  }

  "A piped authenticator" should {
    authenticatorTestSuite(Props[PipedAuthManager])
  }


  def authenticatorTestSuite(props: Props): Unit ={
    import AuthManager._

    "fail to authenticate a non-registered user" in {
      val authManager = system.actorOf(props)
      authManager ! Authenticate("tomasz", "password")
      expectMsg(AuthFailure(AUTH_FAILURE_NOT_FOUND))
    }

    "fail to authenticate if invalid password" in {
      val authManager = system.actorOf(props)
      authManager ! RegisterUser("tomasz", "secret")
      authManager ! Authenticate("tomasz", "password")
      expectMsg(AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT))
    }

    "succesfully authenticate a registered user" in {
      val authManager = system.actorOf(props)
      authManager ! RegisterUser("tomasz", "secret")
      authManager ! Authenticate("tomasz", "secret")
      expectMsg(AuthSuccess)
    }
  }


}
