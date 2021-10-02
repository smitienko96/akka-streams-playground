package com.smitie
package part3_graphs

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, MergePreferred, RunnableGraph, Source}
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy}

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

  RunnableGraph.fromGraph(bufferedRepeater).run()

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
}
