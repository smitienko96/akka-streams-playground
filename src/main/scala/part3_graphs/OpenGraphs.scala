package com.smitie
package part3_graphs

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, Sink, Source}
import akka.stream.{ActorMaterializer, FlowShape, SinkShape, SourceShape}

object OpenGraphs extends App {

  implicit val system = ActorSystem("OpenGraphs")
  implicit val materiakizer = ActorMaterializer()


  /*
    A composite source that concatenates 2 sources with
    - emits ALL elements from the first source
    - then ALL elements from the second
   */
  val firstSource = Source(1 to 10)
  val secondSource = Source(42 to 1000)
  val sourceGraph = Source.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val concat = builder.add(Concat[Int](2))

      firstSource ~> concat
      secondSource ~> concat

      SourceShape(concat.out)
    }
  )


  //  sourceGraph.to(Sink.foreach(println)).run()


  val sink1 = Sink.foreach[Int](x => s"Meaningful thing 1: $x")
  val sink2 = Sink.foreach[Int](x => s"Meaningful thing 2: $x")

  val sinkGraph = Sink.fromGraph(
    GraphDSL.create() { implicit builder =>

      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Int](2))


      broadcast ~> sink1
      broadcast ~> sink2

      SinkShape(broadcast.in)

    }
  )

  //  secondSource.to(sinkGraph).run()

  val additionFlow = Flow[Int].map(_ + 1)
  val multiplicationBy10Flow = Flow[Int].map(_ * 10)

  val flowGraph = Flow.fromGraph(
    GraphDSL.create() { implicit builder =>

      import GraphDSL.Implicits._
      val incrementerShape = builder.add(additionFlow)
      val multiplicationShape = builder.add(multiplicationBy10Flow)
      incrementerShape ~> multiplicationShape

      FlowShape(incrementerShape.in, multiplicationShape.out)

    }
  )

  Source(1 to 20).via(flowGraph).to(Sink.foreach(println)).run()


  def fromSinkAndSource[A, B](sink: Sink[A, _], source: Source[B, _]): Flow[A, B, _] =
    Flow.fromGraph(
      GraphDSL.create() { implicit builder =>

        val sourceShape = builder.add(source)
        val sinkShape = builder.add(sink)

        FlowShape(sinkShape.in, sourceShape.out)
      }
    )

  val f = Flow.fromSinkAndSourceCoupled(Sink.foreach[String](println), Source(1 to 10))


}
