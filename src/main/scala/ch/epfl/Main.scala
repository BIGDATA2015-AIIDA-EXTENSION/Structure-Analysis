package ch.epfl

import ch.epfl.comparison.Comparator
import ch.epfl.structure.StructureParser

import scala.io.Source

object Main {

  def main(args: Array[String]) {
    val fileName = getClass getResource "/comparator_test.json"
    val source = Source fromURL fileName

    val structs = source getLines() flatMap StructureParser.parse

    val s0 = structs find (_.id == "54f582b401162b54f25762ae")
    Comparator.getCompData(s0.get) foreach {
      case ((spec1, spec2), dists) =>
        println(s"($spec1, $spec2) -> $dists")
    }
  }
}
