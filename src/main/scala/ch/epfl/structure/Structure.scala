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
  /*
  Converts structure from ivano.tar.gz aiida db

  Done:
structure.sites.abc.Seq[Double]
structure.sites.xyz.Seq[Double]
structure.lattice.matrix.Seq[Seq[Double]] structure.lattice.volume.[Double]
Missing but used in comparator:
specimen
nbElement (nelements)
   */
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

  //def convertIvanoSite(site: SiteIvano) = {
  def convertIvanoSite(ivanoStructure: StructureIvano) = {
      val abc: Seq[(Double)] = ivanoStructure.sites(0).position
      val xyz: Seq[(Double)] = ivanoStructure.sites(1).position

      // is it correct to map both Tc to abc, xyz, is it always just 2 Tc

      val species = null

      Site(abc,xyz,species)::Nil
  }

  def convertIvanoStruct(ivanoStructure: StructureIvano) = {
    val    gamma = 0.0
    val    a = 0.0
    val    b = 0.0
    val    c = 0.0
    val    matrix = ivanoStructure.cell
    val    volume = ivanoStructure.cellVolume
    val    alpha = 0.0
    val    beta = 0.0

    val lattice = Lattice(gamma, a, b, c, matrix, volume, alpha, beta)

//    val abc: Seq[(Double)] = ivanoStructure.sites(0).position
//    val xyz: Seq[(Double)] = ivanoStructure.sites(1).position
//
//    val species = null
//
//    val sites: Seq[(Site)] = Site(abc,xyz,species)::Nil
    //----
//    val sitesa: Seq[Sites] = ivanoStructure.sites.foreach {convertIvanoSite}

//    val sites: Seq[(Site)] = convertIvanoSite(ivanoStructure.sites(0))::convertIvanoSite(ivanoStructure.sites(1))



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
