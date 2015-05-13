package ch.epfl.structure

import breeze.linalg.{ DenseVector, DenseMatrix }

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
object SpaceGroup {
  val empty: SpaceGroup = SpaceGroup("", "", "", "", "", 0)
}


case class Struct(sites: Seq[Site], lattice: Lattice) {
  def scaled: Struct = {
    val nbSites = sites.length
    val factor = Math.cbrt(nbSites / lattice.volume)
    val newLattice = lattice * factor
    val latticeMatrix = DenseMatrix.tabulate(3, 3) { case (i, j) =>
      newLattice.matrix(i)(j)
    }
    copy(
      sites = sites map (_ * (latticeMatrix, factor)),
      lattice = newLattice)
  }
}

case class Site(abc: Seq[Double], xyz: Seq[Double], species: Seq[Species]) {
  def *(matrix: DenseMatrix[Double], factor: Double) = {
    copy(xyz = (matrix * DenseVector(abc.toArray)).toArray.toList)
  }
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
