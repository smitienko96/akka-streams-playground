package com.smitie
package part4_patterns

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object FaultTolerance extends App {

  implicit val system = ActorSystem("FaultTolerance")
  implicit val materialzier = ActorMaterializer()



}
