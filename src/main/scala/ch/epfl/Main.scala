package ch.epfl

import ch.epfl.structure.{StructureParserIvano, StructureParser}

import scala.io.Source

object Main {
  def main(args: Array[String]) {
//    val fileName1 = getClass getResource "/structures.json"
//
//    val source1 = Source fromURL fileName1
//
//    val structs1 = source1 getLines() flatMap StructureParser.parse
//    structs1 foreach println

// ------
    val fileName2 = getClass getResource "/structures_ivano.json"
    val source2 = Source fromURL fileName2

//    val structs2 = source2 getLines() flatMap { x: String =>
//          println(x)
//          StructureParserIvano.parse(x)
//        }
//    structs2 foreach println

    val test = source2.getLines().next()
    val test1 = StructureParserIvano.parse(test)

    println(test1)

  }
}
