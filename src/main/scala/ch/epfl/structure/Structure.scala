package ch.epfl.structure

case class Structure(id: String, elements: Seq[String], energy: Double, pressure: Double, spaceGroup: SpaceGroup, unitCellFormula: Map[String, Double], struct: Struct, reducedCellFormula: Map[String, Double], nbElements: Double, nbSites: Double, chemsys: String, potential: Potential)

case class SpaceGroup(pointGroup: String, source: String, crystalSystem: String, hall: String, symbol: String, number: Double)

case class Struct(sites: Seq[Site], lattice: Lattice)

case class Site(abc: Seq[Double], xyz: Seq[Double], species: Seq[Species])

case class Species(occu: Double, element: String)

case class Lattice(gamma: Double, a: Double, b: Double, c: Double, matrix: Seq[Seq[Double]], volume: Double, alpha: Double, beta: Double)

case class Potential(name: String, params: Params)

case class Params(aa: Param, bb: Param, ab: Param)

case class Param(cut: Double, epsilon: Double, m: Double, n: Double, sigma: Double)