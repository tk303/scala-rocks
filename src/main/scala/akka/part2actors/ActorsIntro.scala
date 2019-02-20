package akka.part2actors

import akka.actor.{Actor, ActorSystem, Props}

object ActorsIntro extends App {

  // part1 - actor system
  val actorSystem = ActorSystem("firstActorSystem")
  println(actorSystem.name)

  // part2 - create actors
  // word count actor

  class WordCountActor extends Actor {
    // internal data
    var totalWords = 0

    // behaviour
    def receive: PartialFunction[Any, Unit] = {
      case message: String =>
        println(s"[word counter] I have received: ${message}")
        totalWords += message.split(" ").length
      case msg => println(s"[ord counter] I cannot understand ${msg.toString}")
    }
  }

  // part 3 -- instantiate our actor
  val wordCounter = actorSystem.actorOf(Props[WordCountActor], "wordCounter")
  val anotherWOrdCounter = actorSystem.actorOf(Props[WordCountActor], "anotherWOrdCounter")

  // part4 - communicate
  wordCounter ! "I am learning Akka and it's pretty damn cool!"
  anotherWOrdCounter ! "A different message"
  // all is asynchronous!

  class Person(name: String) extends Actor {
    override def receive: Receive = {
      case "hi" => println(s"Hi, ny name is ${name}")
      case _ =>
    }
  }

  // legal (but discaraged way to create an actor of person
  val person = actorSystem.actorOf(Props(new Person("Bob")))
  person ! "hi"

  // best practice - declare a companion object
  object Person {
    def props(name: String) = Props(new Person(name))
  }

  val personBest = actorSystem.actorOf(Person.props("Bobby"))
  personBest ! "hi"

}
