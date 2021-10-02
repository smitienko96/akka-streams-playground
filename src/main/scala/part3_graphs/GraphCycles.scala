package com.smitie
package part3_graphs

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, MergePreferred, RunnableGraph, Sink, Source, Zip}
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy, UniformFanInShape}

object GraphCycles extends App {

  implicit val system = ActorSystem("GraphCycles")
  implicit val materializer = ActorMaterializer()

  val accelerator = GraphDSL.create() { implicit builder =>

    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))
    val incrementerShape = builder.add(Flow[Int].map { x =>
      println(s"Accelerating $x")
      x + 1
    })

    sourceShape ~> mergeShape ~> incrementerShape
    mergeShape <~ incrementerShape

    ClosedShape

  }

  //  RunnableGraph.fromGraph(accelerator).run()
  // graph cycle deadlock

  /**
   * Solution 1: MergePreferred
   */


  val mergePreferredAccelerator = GraphDSL.create() { implicit builder =>

    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(MergePreferred[Int](1))

    val incrementerShape = builder.add(Flow[Int].map { x =>
      println(s"Accelerating $x")
      x + 1
    })

    sourceShape ~> mergeShape ~> incrementerShape
    mergeShape.preferred <~ incrementerShape

    ClosedShape

  }

  //  RunnableGraph.fromGraph(mergePreferredAccelerator).run()


  /**
   * Solution 2: buffers
   */

  val bufferedRepeater = GraphDSL.create() { implicit builder =>

    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))

    val repeaterShape = builder.add(Flow[Int].buffer(10, OverflowStrategy.dropHead).map { x =>
      println(s"Accelerating $x")
      Thread.sleep(100)
      x
    })

    sourceShape ~> mergeShape ~> repeaterShape
    mergeShape <~ repeaterShape

    ClosedShape
  }

//  RunnableGraph.fromGraph(bufferedRepeater).run()

  /**
   * cycles risk deadlocking
   * - add bounds to the number of elements
   * boundedness vs liveness
   */


  /**
   * Challenge: create a fan-in shape
   * - two inputs which will be fed with EXACTLY ONE number
   * - output will emit infinite fibonacci sequence
   */

  val fibonacciGenerator = GraphDSL.create() { implicit builder =>

    import GraphDSL.Implicits._

    val zip = builder.add(Zip[BigInt, BigInt])
    val mergePreferred = builder.add(MergePreferred[(BigInt, BigInt)](1))
    val fiboLogic = builder.add(Flow[(BigInt, BigInt)].map { pair =>
      val last = pair._1
      val previous = pair._2
      Thread.sleep(100)
      (last + previous, last)
    })

    val broadcast = builder.add(Broadcast[(BigInt, BigInt)](2))
    val extractLast = builder.add(Flow[(BigInt, BigInt)].map(_._1))

    zip.out ~> mergePreferred ~> fiboLogic ~> broadcast ~> extractLast

    mergePreferred.preferred <~ broadcast

    UniformFanInShape(extractLast.out, zip.in0, zip.in1)

  }


  val fiboGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val source1Shape = builder.add(Source.single[BigInt](1))
      val source2Shape = builder.add(Source.single[BigInt](1))
      val sink = builder.add(Sink.foreach[BigInt](println))
      val fibo = builder.add(fibonacciGenerator)

      source1Shape ~> fibo.in(0)
      source2Shape ~> fibo.in(1)

      fibo.out ~> sink

      ClosedShape

    }
  )

  fiboGraph.run()

}
