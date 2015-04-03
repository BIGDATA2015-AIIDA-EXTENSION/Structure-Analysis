package ch.epfl

import ch.epfl.structure.StructureParser

import scala.io.Source

object Main {
  def main(args: Array[String]) {
    val fileName = getClass getResource "/structures.json"
    val source = Source fromURL fileName

    val structs = source getLines() flatMap StructureParser.parse
    structs foreach println
  }
}
