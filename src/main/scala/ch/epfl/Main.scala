package ch.epfl

import ch.epfl.computations.AiidaComputations._
import ch.epfl.computations.GraphMaker._

object Main {
  def main(args: Array[String]) {

    args(2) match {
      case "c" => compute(args)
      case "p" => generatePlots(args)
      case _ => println("Missing argument.")
    }



  }
}
