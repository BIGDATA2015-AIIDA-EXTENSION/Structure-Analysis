package ch.epfl.structure

case class Structure(
    id: String,
    elements: Seq[String],
    energy: Double,
    pressure: Double,
    spaceGroup: SpaceGroup,
    unitCellFormula: Map[String, Int],
    struct: Struct,
    reducedCellFormula: Map[String, Int],
    nbElements: Int,
    nbSites: Int,
    chemsys: String,
    potential: Potential,
    prettyFormula: String,
    anonymousFormula: String,
    energyPerSite: Double){

}

object Structure {
  def convertIvano(ivanoStructure: StructureIvano) = {
    val id = ivanoStructure.uuid
    val elements = ???
    val energy = ???
    val pressure = ???
    val spaceGroup = ???
    val unitCellFormula = ???
    val struct = convertIvanoStruct()
    val reducedCellFormula = ???
    val nbElements = ???
    val nbSites = ???
    val chemsys = ???
    val potential = ???
    val prettyFormula = ???
    val anonymousFormula = ???
    val energyPerSite = ???

    Structure(id, elements, energy, pressure, spaceGroup,
    unitCellFormula, struct, reducedCellFormula, nbElements,
    nbSites, chemsys, potential, prettyFormula, anonymousFormula,
    energyPerSite)


}
  def convertIvanoSite(site: SiteIvano) = {
    ???
  }

  def convertIvanoStruct() = {
    ???
  }
}



case class SpaceGroup(pointGroup: String,
    source: String,
    crystalSystem: String,
    hall: String,
    symbol: String,
    number: Int)

case class Struct(sites: Seq[Site], lattice: Lattice)

case class Site(abc: Seq[Double], xyz: Seq[Double], species: Seq[Species])

case class Species(occu: Double, element: String)

case class Lattice(gamma: Double,
    a: Double,
    b: Double,
    c: Double,
    matrix: Seq[Seq[Double]],
    volume: Double,
    alpha: Double,
    beta: Double)

case class Potential(name: String, params: Params)

case class Params(aa: Param, bb: Param, ab: Param)

case class Param(cut: Double, epsilon: Double, m: Int, n: Int, sigma: Double)
