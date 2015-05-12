package ch.epfl.structure

import org.scalatest.FunSuite

import scala.io.Source

/**
 * Created by renucci on 12/05/15.
 */
class NaturalStructureParserTest extends FunSuite {

  test("The parser should correctly parse the whole sample file") {
    val fileName = getClass getResource "/structures.json"
    val source = Source fromURL fileName
    val lines = source.getLines().toList
    val structs = lines flatMap StructureParser.parse
    assert(lines.size == structs.size)
  }

}
