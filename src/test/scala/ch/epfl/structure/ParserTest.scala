package ch.epfl.structure

import org.scalatest.FunSuite

import scala.io.Source

/**
 * Created by renucci on 07/05/15.
 */
class ParserTest extends FunSuite {

  test("The parser should return None on invalid input") {
    assert((StructureParser parse "invalid") == None, "The parser should return None on invalid input")
  }

  test("The parser should correctly parse the whole sample file") {
    val fileName = getClass getResource "/structures.json"
    val source = Source fromURL fileName
    val lines = source.getLines().toList
    val structs = lines flatMap StructureParser.parse
    assert(lines.size == structs.size)
  }

}
