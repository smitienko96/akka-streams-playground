package com.smitie
package part4_patterns

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}

import java.util.Date


object AdvancedBackpressure extends App {

  implicit val system = ActorSystem("AdvancedBackpressure")
  implicit val materializer = ActorMaterializer()


  // control backpressure

  val controlledFlow = Flow[Int].map(_ * 2).buffer(10, OverflowStrategy.dropHead)


  case class PagerEvent(description: String, date: Date, nInstances: Int = 1)

  case class Notification(email: String, event: PagerEvent)

  val events = List(
    PagerEvent("Service discovery failed", new Date),
    PagerEvent("Illegal elements in the data pipeline", new Date),
    PagerEvent("Number of HTTP 500 spiked", new Date),
    PagerEvent("A service stopped responding", new Date)
  )

  val eventSource = Source(events)

  val oncallEngineer = "ssmitienko@mail.com"

  def sendEmail(notification: Notification) =
    println(s"Dear ${notification.email} you have an event: ${notification.event}")


  val notificationsSink = Flow[PagerEvent].map { event => Notification(oncallEngineer, event) }
    .to(Sink.foreach[Notification](sendEmail))

  // standard
//  eventSource.to(notificationsSink).run()

  /**
   * Un-backpressurable source
   */
  def sendEmailSlow(notification: Notification): Unit = {
    Thread.sleep(1000)
    println(s"Dear ${notification.email} you have an event: ${notification.event}")
  }

  val aggregateNotificationFlow = Flow[PagerEvent]
    .conflate((ev1, ev2) => {
      val nInstances = ev1.nInstances + ev2.nInstances
      PagerEvent(s"You have $nInstances events that require your attention", new Date, nInstances)
    })
    .map(resultingEvent => Notification(oncallEngineer, resultingEvent))

//  eventSource.via(aggregateNotificationFlow).to(Sink.foreach[Notification](println)).run()

  import scala.concurrent.duration._

  val slowCounter = Source(Stream.from(1)).throttle(1, 1 second)

  val hungrySink = Sink.foreach[Int](println)

  val extrapolator = Flow[Int].extrapolate(element => Iterator.from(element))

  val repeater = Flow[Int].extrapolate(element => Iterator.continually(element))

  val expander = Flow[Int].expand(element => Iterator.from(element))

//  slowCounter.via(extrapolator).to(hungrySink).run()
//  slowCounter.via(repeater).to(hungrySink).run()
  slowCounter.via(expander).to(hungrySink).run()


}
