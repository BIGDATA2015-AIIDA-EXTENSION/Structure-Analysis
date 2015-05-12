package ch.epfl.structure

case class Structure(id: String,
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
                     energyPerSite: Double) {
  def scaled: Structure = copy(struct = struct.scaled)
}


case class SpaceGroup(pointGroup: String,
                      source: String,
                      crystalSystem: String,
                      hall: String,
                      symbol: String,
                      number: Int)


case class Struct(sites: Seq[Site], lattice: Lattice) {
  def scaled: Struct = {
    val nbSites = sites.length
    val factor = nbSites / Math.cbrt(lattice.volume)
    copy(
      sites = sites map (_ * factor),
      lattice = lattice * factor)
  }
}

case class Site(abc: Seq[Double], xyz: Seq[Double], species: Seq[Species]) {
  def *(factor: Double) = copy(xyz = xyz map (_ * factor))
}

case class Species(occu: Double, element: String)

case class Lattice(gamma: Double,
                   a: Double,
                   b: Double,
                   c: Double,
                   matrix: Seq[Seq[Double]],
                   volume: Double,
                   alpha: Double,
                   beta: Double) {
  def *(factor: Double): Lattice = {
    copy(
      a = a * factor,
      b = b * factor,
      c = c * factor,
      matrix = matrix map (_ map (_ * factor)),
      volume = a * b * c * factor * factor * factor)
  }
}

case class Potential(name: String, params: Params)

case class Params(aa: Param, bb: Param, ab: Param)

case class Param(cut: Double, epsilon: Double, m: Int, n: Int, sigma: Double)
