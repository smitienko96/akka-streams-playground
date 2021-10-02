package com.smitie
package part3_graphs

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, BidiShape, ClosedShape}

object BidirectionalFlows extends App {

  implicit val system = ActorSystem("BidirectionalFlows")
  implicit val materializer = ActorMaterializer()

  /**
   * Example: cryptography
   *
   */


  def encrypt(n: Int)(string: String) =  string.map(c => (c + n).toChar)

  def decrypt(n: Int)(string: String) =  string.map(c => (c - n).toChar)


//  println(encrypt(12)("Akka"))
//  println(decrypt(12)(encrypt(12)("Akka")))

  val bidiCryptoStaticGraph= GraphDSL.create() { implicit builder =>
    val encryptionFlowShape = builder.add(Flow[String].map(encrypt(3)))
    val decryptionFlowShape = builder.add(Flow[String].map(decrypt(3)))

    BidiShape.fromFlows(encryptionFlowShape, decryptionFlowShape)

  }

  val unencryptedStrings= List("akka", "is", "awesome", "testing", "bidirectional", "flows")
  val unencryptedSource  = Source(unencryptedStrings)
  val encryptedSource = Source(unencryptedStrings.map(encrypt(3)))

  val cryptoBidiGraph = RunnableGraph.fromGraph(
    GraphDSL.create() {implicit builder =>
      import GraphDSL.Implicits._

      val unencryptedSourceShape = builder.add(unencryptedSource)
      val encryptedSourceShape = builder.add(encryptedSource)

      val bidi = builder.add(bidiCryptoStaticGraph)

      val encryptedSinkShape = builder.add(Sink.foreach[String](x => println(s"Encrypted: $x")))
      val decryptedSinkShape = builder.add(Sink.foreach[String](x => println(s"Decrypted: $x")))


      unencryptedSourceShape ~> bidi.in1 ; bidi.out1 ~> encryptedSinkShape
      decryptedSinkShape <~ bidi.out2; bidi.in2 <~ encryptedSourceShape

      ClosedShape
    }
  )

  cryptoBidiGraph.run()

}
