package com.smitie
package part4_patterns

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source

import java.util.Date
import scala.concurrent.Future

object IntegratingWithExternalServices extends App {

  implicit val system = ActorSystem("IntegratingWithExternalServices")
  implicit val materializer = ActorMaterializer()

  case class PagerEvent(application: String, description: String, date: Date)

  val eventSource = Source(List(
    PagerEvent("AkkaInfra", "Infrastructure broke", new Date),
    PagerEvent("FastDataPipeline", "Illegal elements in the data pipeline ", new Date),
    PagerEvent("AkkaInfra", "A service stopped responding", new Date),
    PagerEvent("SuperFrontend", "A button doesn't work", new Date),
  ))


  object PageService {
    private val engineers = List("Sergei", "John", "Lady Gaga")
    private val emails = Map(
      "Sergei" -> "sergeis@gmail.com",
      "John" -> "johngolt@gmail.com",
      "Lady Gaga" -> "ladygaga@gmail.com"
    )
    import system.dispatcher
    def processEvent(pagerEvent: PagerEvent) = Future {
     val engineerIndex = ???
    }
  }
}
