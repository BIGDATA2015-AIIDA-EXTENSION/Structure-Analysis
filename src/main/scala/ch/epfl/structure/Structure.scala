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
    energyPerSite: Double)

object Structure {
  def convertIvano(ivanoStructure: StructureIvano) = {
    val id = ivanoStructure.uuid
    val elements = null
    val energy = 0.0
    val pressure = 0.0
    val spaceGroup = null
    val unitCellFormula = null
    val struct = convertIvanoStruct(ivanoStructure)
    val reducedCellFormula = null
    val nbElements = 0
    val nbSites = 0
    val chemsys = null
    val potential = null
    val prettyFormula = null
    val anonymousFormula = null
    val energyPerSite = 0

    Structure(id, elements, energy, pressure, spaceGroup,
    unitCellFormula, struct, reducedCellFormula, nbElements,
    nbSites, chemsys, potential, prettyFormula, anonymousFormula,
    energyPerSite)

  }

  def convertIvanoSite(ivanoStructure: StructureIvano) = {

      ivanoStructure.sites map {
        case SiteIvano(position, kindName, _) =>
          Site(List(0, 0, 0), position, List(Species(0, kindName)))
      }
  }

  def convertIvanoStruct(ivanoStructure: StructureIvano) = {
    val gamma = 0.0
    val a = ivanoStructure.cellLengths(0)
    val b = ivanoStructure.cellLengths(1)
    val c = ivanoStructure.cellLengths(2)
    val matrix = ivanoStructure.cell
    val volume = ivanoStructure.cellVolume
    val alpha = 0.0
    val beta = 0.0

    val lattice = Lattice(gamma, a, b, c, matrix, volume, alpha, beta)


    Struct(convertIvanoSite(ivanoStructure), lattice)
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

case class Lattice(
                    gamma: Double,
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
