package ch.epfl.comparison

import breeze.linalg._
import ch.epfl.structure._

object Comparator {

  type ComparisonData = Map[(String, String), Seq[Double]]

  val MAX_CELL_MULTIPLES = 256
  val MAX_VALUES         = 10000
  val CUTOFF_FACTOR      = 1.5
  val TOLERANCE          = 3e-4

  /**
   * @return true if two structure are similar, false otherwise
   */
  def areSimilar(s1: Structure, s2: Structure): Boolean =
    areComparable(s1, s2) && areSimilar(getCompData(s1), getCompData(s2))

  /**
   * @return true if two comparison data are similar, false otherwise
   */
  def areSimilar(ds1: ComparisonData, ds2: ComparisonData): Boolean =
    areComparable(ds1, ds2) && distance(ds1, ds2) < TOLERANCE

  /**
   * @return true if two structures are comparable, false otherwise
   */
  def areComparable(s1: Structure, s2: Structure): Boolean =
    s1.nbSites == s2.nbSites && s1.prettyFormula == s2.prettyFormula

  /**
   * @return true if two comparison data are comparable, false otherwise
   */
  def areComparable(ds1: ComparisonData, ds2: ComparisonData): Boolean =
    ds1.keySet == ds2.keySet

  /**
   * @return the distance between two structures
   * @throws IllegalArgumentException if the structures are not comparable
   */
  def distance(s1: Structure, s2: Structure): Double = {
    require(areComparable(s1, s2), "Structure are not comparable")
    distance(getCompData(s1), getCompData(s2))
  }

  /**
   * @return the distance between two comparison data
   * @throws IllegalArgumentException if the comparison datas are not comparable
   */
  def distance(ds1: ComparisonData, ds2: ComparisonData): Double = {
    require(areComparable(ds1, ds2), "Comparison datas are not comparable")

    // Difference between the two sets of sequences
    val diff = ds1.toList flatMap {
      case (spec, dist1) =>
        val dist2 = ds2(spec)
        val min = dist1.size min dist2.size
        val v1 = dist1 take min
        val v2 = dist2 take min

        ((v1 zip v2) foldLeft List.empty[Double]) {
          case (acc, (d1, d2)) =>
            val sum = d1 + d2
            if (sum > 0) (2 * Math.abs(d1 - d2) / sum) :: acc
            else acc
        }
    }

    // Root mean square
    Math.sqrt(diff.map(Math.pow(_, 2)).sum / diff.length)
  }

  /**
   * @return the data used for comparison between structures
   */
  def getCompData(s: Structure): ComparisonData = {
    val unitCell = new UnitCell(s.struct.lattice)
    val sites = s.struct.sites
    val combinations = for {
      s1 <- sites
      s2 <- sites
    } yield (s1, s2)

    // Generating the list of distances (up to some maximum) between atoms,
    // one for each pair of species
    val dists = combinations.toSeq flatMap { case (i, j) => getDistsBetween(i, j, unitCell) }

    // Group the computed distances by pair of species
    val distsBySpecies = dists groupBy {
      case ((spec1, spec2), _) =>
        if (spec1 <= spec2) (spec1, spec2)
        else (spec2, spec1) // B~A goes to A~B
    }

    // Sort the distances
    distsBySpecies map { case (k, v) => (k, v.flatMap(_._2).sorted) }
  }

  /**
   * @return the distances between two atoms
   */
  private def getDistsBetween(s1: Site, s2: Site, unitCell: UnitCell): Seq[((String, String), Seq[Double])] = {
    val cutoff = unitCell.cutoff
    val a = DenseVector(s1.xyz.toArray)
    val b = DenseVector(s2.xyz.toArray)

    val dR = b - a

    val aMax = unitCell.aMax(dR)
    val bMax = unitCell.bMax(dR)
    val cMax = unitCell.cMax(dR)

    val cutoffSq = cutoff * cutoff

    // Lazy evaluated since we want to compute distances only up to some maximum
    val resultStream = for {
      a <- (-aMax to aMax).toStream
      rA = unitCell.a * a.toDouble
      b <- -bMax to bMax
      rAB = rA + unitCell.b * b.toDouble
      c <- -cMax to cMax
      outVec = rAB + unitCell.c * c.toDouble + dR
      testDistSq = outVec dot outVec
      if testDistSq < cutoffSq
    } yield Math.sqrt(testDistSq)

    val results = resultStream take MAX_VALUES

    // One for each pair of species (A~A, A~B, B~A, B~B)
    for {
      spec1 <- s1.species
      spec2 <- s2.species
    } yield (spec1.element, spec2.element) -> results
  }

  /**
   * Used locally as a cache and an helper class
   */
  private class UnitCell(lattice: Lattice) {

    val a = DenseVector(lattice.matrix(0).toArray)
    val b = DenseVector(lattice.matrix(1).toArray)
    val c = DenseVector(lattice.matrix(2).toArray)
    val volume = lattice.volume

    val aCrossB = cross(a, b)
    val aCrossBLen = Math.sqrt(aCrossB dot aCrossB)
    val aCrossBHat = aCrossB / aCrossBLen

    val bCrossC = cross(b, c)
    val bCrossCLen = Math.sqrt(bCrossC dot bCrossC)
    val bCrossCHat = bCrossC / bCrossCLen

    val aCrossC = cross(a, c)
    val aCrossCLen = Math.sqrt(aCrossC dot aCrossC)
    val aCrossCHat = aCrossC / aCrossCLen

    val cutoff = CUTOFF_FACTOR * max(lattice.a, lattice.b, lattice.c)

    private def helper(crossHat: DenseVector[Double], crossLen: Double)(dR: DenseVector[Double]) = {
      val radius = cutoff + Math.abs(dR dot crossHat)
      (Math.floor(radius / volume * crossLen) min MAX_CELL_MULTIPLES).toInt
    }

    def aMax(dR: DenseVector[Double]): Int = helper(bCrossCHat, bCrossCLen)(dR)

    def bMax(dR: DenseVector[Double]): Int = helper(aCrossCHat, aCrossCLen)(dR)

    def cMax(dR: DenseVector[Double]): Int = helper(aCrossBHat, aCrossBLen)(dR)
  }

}
