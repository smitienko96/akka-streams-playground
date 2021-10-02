package com.smitie
package part3_graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}
import akka.stream.{ActorMaterializer, ClosedShape}

object GraphBasics extends App {

  implicit val sys: ActorSystem = ActorSystem("graph-basics")

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val input = Source(1 to 1000)
  val incrementer = Flow[Int].map(v => v + 1)
  val multiplier = Flow[Int].map(v => v * 10)

  val output = Sink.foreach[(Int, Int)](println)

  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Int](2))
      val zip = builder.add(Zip[Int, Int])

      input ~> broadcast

      broadcast.out(0) ~> incrementer ~> zip.in0
      broadcast.out(1) ~> multiplier ~> zip.in1

      zip.out ~> output

      ClosedShape

    }
  )

  //  graph.run()

  val firstSink = Sink.foreach[Int](i => println(s"[${Thread.currentThread().getName}: first sink]: $i"))
  val secondSink = Sink.foreach[Int](i => println(s"[${Thread.currentThread().getName}: second sink]: $i"))

  val twoSinksGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[Int](2))

      input.async ~> broadcast

      broadcast.out(0) ~> firstSink
      broadcast.out(1) ~> secondSink


      ClosedShape
    }
  )

  //  twoSinksGraph.run()

  import scala.concurrent.duration._

  val fastSource = input.throttle(5, 1 second)
  val slowSource = input.throttle(2, 1 second)

  val sink1 = Sink.fold[Int, Int](0) { (count, _) =>
    println(s"Sink 1 number of elements: $count")
    count + 1
  }
  val sink2 = Sink.fold[Int, Int](0) { (count, _) =>
    println(s"Sink 2 number of elements: $count")
    count + 1
  }

  val fastSourceSlowSourceGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val merge = builder.add(Merge[Int](2))
      val balance = builder.add(Balance[Int](2))


      fastSource ~> merge ~> balance ~> sink1
      slowSource ~> merge;
      balance ~> sink2

      ClosedShape

    }
  )

  fastSourceSlowSourceGraph.run()


}
