import org.scalatest.FunSuite
import ch.epfl.structure.StructureParser
import scala.io.Source

class ValidJSONTest extends FunSuite {
  test("The parser should correctly parse the whole sample file") {
    val fileName = getClass getResource "/structures.json"
    val source = Source fromURL fileName
    val lines = source.getLines().toList
    val structs = lines flatMap StructureParser.parse
    assert(lines.size == structs.size)
  }
}
