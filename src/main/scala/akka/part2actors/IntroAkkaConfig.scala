package akka.part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object IntroAkkaConfig extends App {

  class SimpleLoggingActor extends Actor with ActorLogging {

    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  /**
    *  1 - inline configuration
    */
  val configString =
    """
       akka {
        loglevel = "INFO"
       }
    """.stripMargin

  val config = ConfigFactory.parseString(configString)
  val system = ActorSystem("ConfigurationDemo", ConfigFactory.load(config))
  val actor = system.actorOf(Props[SimpleLoggingActor])
  actor ! "A message to remember"

  /**
    *  2 - Default config file
    */

  val defaultConfigFileSystem = ActorSystem("DefaultConfigFileDemo")
  val defaultConfigActor = defaultConfigFileSystem.actorOf(Props[SimpleLoggingActor])
  defaultConfigActor ! "Remember me"

  /**
    * 3 - separate configuration in the same file
    */
  val specialConfig = ConfigFactory.load.getConfig("mySpecialConfig")
  val specialConfigSystem = ActorSystem("SpecialConfigDemo", specialConfig)
  val specialConfigActor = specialConfigSystem.actorOf(Props[SimpleLoggingActor])
  specialConfigActor ! "I'm special"

  /**
    *  4 - separate config in another file
    */

  val separateConfig = ConfigFactory.load("secretFolder/secretConfiguration.conf")
  println(s"separate config log level ${separateConfig.getString("akka.loglevel")}")

  /**
    *  5 - different file formats
    *  JSON, Properties
    */
  val jsonConfig = ConfigFactory.load("json/jsonConfig.json")
  println(s"json config, aJsonProperty: ${jsonConfig.getString("aJsonProperty")}")
  println(s"akka loglevel: ${jsonConfig.getString("akka.loglevel")}")

  val propsConfig = ConfigFactory.load("props/propsConfiguration.properties")
  println(s"json config, my.simpleProperty: ${propsConfig.getString("my.simpleProperty")}")
  println(s"akka loglevel: ${propsConfig.getString("akka.loglevel")}")


}
