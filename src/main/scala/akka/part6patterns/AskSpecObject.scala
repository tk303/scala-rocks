package akka.part6patterns

import akka.actor.{Actor, ActorLogging, Props}
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}
// step 1 - import the ask pattern
import akka.pattern.ask

object AskSpecObject {

  case class Read(key: String)
  case class Write(key: String, value: String)

  // this code is somewhere else in your app
  class KeyValueActor extends Actor with ActorLogging {

    override def receive: Receive = online(Map.empty)

    def online(kv: Map[String, String]): Receive = {
      case Read(key) =>
        log.info(s"Trying to read the value at the key: $key")
        sender() ! kv.get(key) // get will return Option[String]
      case Write(key, value) =>
        log.info(s"Writing the value $value for the key: $key")
        context.become(online(kv + (key -> value)))
    }
  }

  // user authenticator actor
  case class RegisterUser(username: String, password: String)
  case class Authenticate(username: String, password: String)
  case class AuthFailure(message: String)
  case object AuthSuccess
  object AuthManager {
    val AUTH_FAILURE_NOT_FOUND = "username not found"
    val AUTH_FAILURE_PASSWORD_INCORRECT = "password incorrect"
    val AUTH_FAILURE_SYSTEM_ERROR = "system error"
  }

  class AuthManager extends Actor with ActorLogging {
    import AuthManager._

    // step 2 - logistics
    implicit val timeout: Timeout = Timeout(1 second)
    implicit val executionContext: ExecutionContext = context.dispatcher

    private val authDB = context.actorOf(Props[KeyValueActor], "authDB")
    override def receive: Receive = {
      case RegisterUser(username, password) => authDB ! Write(username, password)
      case Authenticate(username, password) =>
        val originalSender = sender()
        // step 3 - ask the actor
        val future = authDB ? Read(username)
        // step 4 - handle the future e.g. with onComplete
        future.onComplete{
          // step 5 most important
          // NEVER CALL METHODS ON THE ACTOR INSTANCE OR ACCESS MUTABLE STATE IN ONCOMPLETE
          // avoid closing over the actor instance or mutable state
          case Success(None) => originalSender() ! AuthFailure(AUTH_FAILURE_NOT_FOUND)
          case Success(Some(dbPassword)) =>
            if(dbPassword == password) sender () ! AuthSuccess
            else sender() ! AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
          case Failure(_) => originalSender ! AuthFailure(AUTH_FAILURE_SYSTEM_ERROR)
        }
    }
  }

}
