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
    val struct             = convertIvanoStruct(ivanoStructure)
    val id                 = ivanoStructure.uuid
    val elements           = (struct.sites flatMap (_.species map (_.element)))
    val energy             = 0
    val pressure           = 0
    val spaceGroup         = SpaceGroup.empty
    val unitCellFormula    = Map.empty[String, Int]
    val reducedCellFormula = Map.empty[String, Int]
    val nbElements         = elements.distinct.size
    val nbSites            = struct.sites.size
    val chemsys            = ""
    val potential          = Potential.empty
    val prettyFormula      = ""
    val anonymousFormula   = ""
    val energyPerSite      = 0

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
object SpaceGroup {
  val empty: SpaceGroup = SpaceGroup("", "", "", "", "", 0)
}

case class Struct(sites: Seq[Site], lattice: Lattice)
object Struct {
  val empty: Struct = Struct(Nil, Lattice.empty)
}

case class Site(abc: Seq[Double], xyz: Seq[Double], species: Seq[Species])
object Site {
  val empty: Site = Site(Nil, Nil, Nil)
}

case class Species(occu: Double, element: String)
object Species {
  val empty: Species = Species(0, "")
}

case class Lattice(
                    gamma: Double,
                    a: Double,
                    b: Double,
                    c: Double,
                    matrix: Seq[Seq[Double]],
                    volume: Double,
                    alpha: Double,
                    beta: Double)
object Lattice {
  val empty: Lattice = Lattice(0, 0, 0, 0, Nil, 0, 0, 0)
}

case class Potential(name: String, params: Params)
object Potential {
  val empty: Potential = Potential("", Params.empty)
}

case class Params(aa: Param, bb: Param, ab: Param)
object Params {
  val empty: Params = Params(Param.empty, Param.empty, Param.empty)
}

case class Param(cut: Double, epsilon: Double, m: Int, n: Int, sigma: Double)
object Param {
  val empty: Param = Param(0, 0, 0, 0, 0)
}
