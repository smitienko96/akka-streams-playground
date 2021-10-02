package com.smitie
package part3_graphs

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}
import akka.stream.{ActorMaterializer, ClosedShape, FanOutShape2, UniformFanInShape}

import java.util.Date

object MoreOpenGraphs extends App {

  implicit val system = ActorSystem("MoreOpenGraphs")
  implicit val materializer = ActorMaterializer()

  /**
   * Max3 operator
   * - 3 inputs
   * - the maximum of the 3
   */

  val source1 = Source(1 to 10)
  val source2 = source1.map(_ + 3)
  val source3 = source2.map(_ / 2)

  val max3 = Flow[(Int, Int, Int)].map(out => out._1 max out._2 max out._3)

  val max3StaticGraph = GraphDSL.create() { implicit builder =>


    val zip = builder.add(ZipWith[Int, Int, Int, Int]((a, b, c) => a max b max c))


    UniformFanInShape(zip.out, zip.in0, zip.in1, zip.in2)
  }


  val maxSink = Sink.foreach[Int](x => println(s"Max is: $x"))

  val maxRunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val max3Shape = builder.add(max3StaticGraph)

      source1 ~> max3Shape.in(0)
      source2 ~> max3Shape.in(1)
      source3 ~> max3Shape.in(2)

      max3Shape.out ~> maxSink

      ClosedShape
    }
  )

  //  maxRunnableGraph.run()

  /**
   * Non uniform fan out shape
   *
   * Processing bank transactions
   * Txn suspicious if amount > 100000000
   * Streams component for transactions
   * - output1: let the transaction go through
   * - output2: suspicious transactions ids
   */


  case class Transaction(id: String, source: String, recipient: String, amount: Int, date: Date)

  val transactionSource = Source(List(
    Transaction("536173512", "Sergei", "Daniel", 4000, new Date()),
    Transaction("632876427", "Maga", "Ramzan", 12000, new Date()),
    Transaction("732863428", "Mauricio", "Antonio", 6500, new Date()),
    Transaction("732863423", "Mark", "Albert", 20000, new Date()),
    Transaction("732863242", "Isabella", "Lucas", 8700, new Date())
  ))

  val bankProcessor = Sink.foreach[Transaction](println)
  val suspiciousAnalysisService = Sink.foreach[String](txnId => println(s"Suspicious transaction id: $txnId"))


  val suspiciousTransactionStaticGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val broadcast = builder.add(Broadcast[Transaction](2))
    val suspiciousTxnFilter = builder.add(Flow[Transaction].filter(txn => txn.amount > 10000))
    val txnIdExctractor = builder.add(Flow[Transaction].map[String](txn => txn.id))

    broadcast.out(0) ~> suspiciousTxnFilter ~> txnIdExctractor

    new FanOutShape2(broadcast.in, broadcast.out(1), txnIdExctractor.out)

  }


  val suspiciousTransactionRunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val suspiciousTxnShape = builder.add(suspiciousTransactionStaticGraph)

      transactionSource ~> suspiciousTxnShape.in
      suspiciousTxnShape.out0 ~> bankProcessor
      suspiciousTxnShape.out1 ~> suspiciousAnalysisService

      ClosedShape
    }
  )

  suspiciousTransactionRunnableGraph.run()

  //  case class Transaction(id: String, source: String, recipient: String, amount: Int, date: Date)
  //
  //    val transactionSource = Source(List(
  //      Transaction("536173512", "Sergei", "Daniel", 4000, new Date()),
  //      Transaction("632876427", "Maga", "Ramzan", 12000, new Date()),
  //      Transaction("732863428", "Mauricio", "Antonio", 6500, new Date()),
  //      Transaction("732863428", "Mark", "Albert", 20000, new Date()),
  //      Transaction("732863428", "Isabella", "Lucas", 8700, new Date())
  //    ))
  //
  //  val bankProcessor = Sink.foreach[Transaction](println)
  //  val suspiciousAnalysisService = Sink.foreach[String](txnId => println(s"Suspicious transaction ID: $txnId"))
  //
  //  // step 1
  //  val suspiciousTxnStaticGraph = GraphDSL.create() { implicit builder =>
  //    import GraphDSL.Implicits._
  //
  //    // step 2 - define SHAPES
  //    val broadcast = builder.add(Broadcast[Transaction](2))
  //    val suspiciousTxnFilter = builder.add(Flow[Transaction].filter(txn => txn.amount > 10000))
  //    val txnIdExtractor = builder.add(Flow[Transaction].map[String](txn => txn.id))
  //
  //    // step 3 - tie SHAPES
  //    broadcast.out(0) ~> suspiciousTxnFilter ~> txnIdExtractor
  //
  //    // step 4
  //    new FanOutShape2(broadcast.in, broadcast.out(1), txnIdExtractor.out)
  //  }
  //
  //  // step 1
  //  val suspiciousTxnRunnableGraph = RunnableGraph.fromGraph(
  //    GraphDSL.create() { implicit builder =>
  //      import GraphDSL.Implicits._
  //
  //      // step 2
  //      val suspiciousTxnShape = builder.add(suspiciousTxnStaticGraph)
  //
  //      // step 3
  //      transactionSource ~> suspiciousTxnShape.in
  //      suspiciousTxnShape.out0 ~> bankProcessor
  //      suspiciousTxnShape.out1 ~> suspiciousAnalysisService
  //
  //      // step 4
  //      ClosedShape
  //    }
  //  )

  //  suspiciousTxnRunnableGraph.run()

}
