import ch.epfl.structure.StructureParser

import scala.io.Source

object Main {
  def main(args: Array[String]) {
    if (args.length < 1) {
      sys error "File name missing!"
    }

    val fileName = args(0)
    val source = Source fromFile fileName
    val structs = source getLines() flatMap StructureParser.parse
    structs foreach println
  }
}
