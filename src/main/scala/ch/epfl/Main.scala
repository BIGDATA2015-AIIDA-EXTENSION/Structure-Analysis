package ch.epfl

import ch.epfl.clustering.structure2d.AtomClustering
import ch.epfl.comparison.Comparison
import ch.epfl.heatmaps.AiidaComputations

object Main {

  def main(args: Array[String]) {
    args(2) match {
      case "c" => AiidaComputations.compute(args)
      case "cl" => AtomClustering.compute(args)
      case "2d" => AtomClustering.compute2d(args)
      case "comparison" => Comparison.run(args)
      case _ => println("Missing argument.")
    }
  }

}
