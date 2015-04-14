import org.scalatest.FunSuite
import ch.epfl.structure.StructureParser

class InvalidJSONTest extends FunSuite {
  test("The parser should return none on invalid input") {
    assert((StructureParser parse "invalid") == None, "The parser should return None on invalid input")
  }
}
