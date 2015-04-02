package ch.epfl.structure

case class Structure(id: String, elements: List[String], energy: Double, pressure: Double, spaceGroup: SpaceGroup, unitCellFormula: Map[String, Double], struct: Struct, reducedCellFormula: Map[String, Double], nbElements: Double, nbSites: Double, chemsys: String, potential: Potential)

case class SpaceGroup(point_group: String, source: String, crystal_system: String, hall: String, symbol: String, number: Double)

case class Struct(sites: List[Site], lattice: Lattice)

case class Site(abc: List[Double], xyz: List[Double], species: List[Species])

case class Species(ocu: Double, element: String)

case class Lattice(gamma: Double, a: Double, b: Double, c: Double, matrix: List[List[Double]], volume: Double, alpha: Double, beta: Double)

case class Potential(name: String, params: Params)

case class Params(aa: Param, bb: Param, ab: Param)

case class Param(cut: Double, epsilon: Double, m: Double, n: Double, sigma: Double)