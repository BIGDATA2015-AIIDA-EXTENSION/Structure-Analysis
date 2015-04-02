package ch.epfl.structure

import scala.util.parsing.json.JSON

object StructureParser {
  type JSONObject   = Map[String, Any]
  type JSONArray[T] = List[T]

  def parse(line: String): Option[Structure] = {
    val Some(json: JSONObject) = JSON parseFull line
    try Some(parseStructure(json))
    catch {
      case e: Exception =>
        e.printStackTrace()
        None
    }
  }

  private def parseStructure(structure: JSONObject): Structure = {
    val _id                 = structure get "_id"
    val _elements           = structure get "elements"
    val _energy             = structure get "energy"
    val _pressure           = structure get "pressure"
    val _spaceGroup         = structure get "spacegroup"
    val _unitCellFormula    = structure get "unit_cell_formula"
    val _struct             = structure get "structure"
    val _reducedCellFormula = structure get "reduced_cell_formula"
    val _nbElements         = structure get "nelements"
    val _nbSites            = structure get "nsites"
    val _chemsys            = structure get "chemsys"
    val _potential          = structure get "potential"

    val (Some(id: JSONObject), Some(elements: JSONArray[String] @unchecked), Some(energy: Double), Some(pressure: Double), Some(spaceGroup: JSONObject), Some(unitCellFormula: Map[String, Double] @unchecked), Some(struct: JSONObject), Some(reducedCellFormula: Map[String, Double] @unchecked), Some(nbElements: Double), Some(nbSites: Double), Some(chemsys: String), Some(potential: JSONObject)) =
      (_id, _elements, _energy, _pressure, _spaceGroup, _unitCellFormula, _struct, _reducedCellFormula, _nbElements, _nbSites, _chemsys, _potential)

    Structure(parseId(id), elements, energy, pressure, parseSpaceGroup(spaceGroup), unitCellFormula, parseStruct(struct), reducedCellFormula, nbElements, nbSites, chemsys, parsePotential(potential))

  }

  private def parseId(idObj: JSONObject): String = {
    val _id = idObj get "$oid"
    val Some(id: String) = _id
    id
  }

  private def parseSpaceGroup(spaceGroup: JSONObject): SpaceGroup = {
    val _pointGroup    = spaceGroup get "point_group"
    val _source        = spaceGroup get "source"
    val _crystalSystem = spaceGroup get "crystal_system"
    val _hall          = spaceGroup get "hall"
    val _symbol        = spaceGroup get "symbol"
    val _number        = spaceGroup get "number"

    val (Some(pointGroup: String), Some(source: String), Some(crystalSystem: String), Some(hall: String), Some(symbol: String), Some(number: Double)) =
      (_pointGroup, _source, _crystalSystem, _hall, _symbol, _number)

    SpaceGroup(pointGroup, source, crystalSystem, hall, symbol, number)
  }

  private def parseStruct(struct: JSONObject): Struct = {
    val _sites   = struct get "sites"
    val _lattice = struct get "lattice"

    val (Some(sites: JSONArray[JSONObject] @unchecked), Some(lattice: JSONObject)) =
      (_sites, _lattice)

    Struct(sites map parseSite, parseLattice(lattice))
  }

  private def parseLattice(latice: JSONObject): Lattice = {
    val _gamma  = latice get "gamma"
    val _a      = latice get "a"
    val _b      = latice get "b"
    val _c      = latice get "c"
    val _matrix = latice get "matrix"
    val _volume = latice get "volume"
    val _alpha  = latice get "alpha"
    val _beta   = latice get "beta"

    val (Some(gamma: Double), Some(a: Double), Some(b: Double), Some(c: Double), Some(matrix: JSONArray[JSONArray[Double]] @unchecked), Some(volume: Double), Some(alpha: Double), Some(beta: Double)) =
      (_gamma, _a, _b, _c, _matrix, _volume, _alpha, _beta)

    Lattice(gamma, a, b, c, matrix, volume, alpha, beta)
  }

  private def parseSite(site: JSONObject): Site = {
    val _abc     = site get "abc"
    val _xyz     = site get "xyz"
    val _species = site get "species"

    val (Some(abc: JSONArray[Double] @unchecked), Some(xyz: JSONArray[Double] @unchecked), Some(speciesList: JSONArray[JSONObject] @unchecked)) =
      (_abc, _xyz, _species)

    Site(abc, xyz, speciesList map parseSpecies)
  }

  private def parsePotential(potential: JSONObject): Potential = {
    val _name   = potential get "name"
    val _params = potential get "params"

    val (Some(name: String), Some(params: JSONObject)) = (_name, _params)
    Potential(name, parseParams(params))
  }

  private def parseSpecies(species: JSONObject): Species = {
    val _occu    = species get "occu"
    val _element = species get "element"

    val (Some(ocu: Double), Some(element: String)) =
      (_occu, _element)

    Species(ocu, element)
  }

  private def parseParams(params: JSONObject): Params = {
    val _aa = params get "A~A"
    val _bb = params get "B~B"
    val _ab = params get "A~B"

    val (Some(aa: JSONObject), Some(bb: JSONObject), Some(ab: JSONObject)) =
      (_aa, _bb, _ab)

    Params(parseParam(aa), parseParam(bb), parseParam(ab))
  }

  private def parseParam(param: JSONObject): Param = {
    val _cut     = param get "cut"
    val _epsilon = param get "epsilon"
    val _m       = param get "m"
    val _n       = param get "n"
    val _sigma   = param get "sigma"

    val (Some(cut: Double), Some(epsilon: Double), Some(m: Double), Some(n: Double), Some(sigma: Double)) =
      (_cut, _epsilon, _m, _n, _sigma)

    Param(cut, epsilon, m, n, sigma)
  }
}
