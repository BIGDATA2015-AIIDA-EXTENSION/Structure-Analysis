package ch.epfl.comparison

import breeze.linalg._
import ch.epfl.structure._

object Comparator {

  type ComparisonData = Map[(Species, Species), Seq[Double]]

  val MAX_CELL_MULTIPLES = 256
  val MAX_VALUES         = 10000
  val CUTOFF_FACTOR      = 1.001
  val TOLERANCE          = 3e-4

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

    val cutoff = CUTOFF_FACTOR * max(norm(a), norm(b), norm(c))

    private def helper(crossHat: DenseVector[Double], crossLen: Double)(dR: DenseVector[Double]) = {
      val radius = cutoff + Math.abs(dR dot crossHat)
      (Math.floor(radius / volume * crossLen) min MAX_CELL_MULTIPLES).toInt
    }

    def aMax(dR: DenseVector[Double]): Int = helper(bCrossCHat, bCrossCLen)(dR)
    def bMax(dR: DenseVector[Double]): Int = helper(aCrossCHat, aCrossCLen)(dR)
    def cMax(dR: DenseVector[Double]): Int = helper(aCrossBHat, aCrossBLen)(dR)
  }

  /**
   * Return true if two structure are similar, false otherwise
   */
  def areSimilar(s1: Structure, s2: Structure): Boolean =
    areSimilar(getCompData(s1), getCompData(s2))

  /**
   * Return true if two comparison data are similar, false otherwise
   */
  def areSimilar(ds1: ComparisonData, ds2: ComparisonData): Boolean =
    distance(ds1, ds2) exists (_ < TOLERANCE)

  /**
   * If possible, compute the distance between two structures
   */
  def distance(s1: Structure, s2: Structure): Option[Double] = {
    if (s1.nbElements != s2.nbElements) None
    else if (s1.elements.toSet != s2.elements.toSet) None
    else distance(getCompData(s1), getCompData(s2))
  }

  /**
   * If possible, compute the distance between two comparison data
   */
  def distance(ds1: ComparisonData, ds2: ComparisonData): Option[Double] = {
    if (ds1.keySet != ds2.keySet) None
    else {
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
              if (sum > 0)  (2 * Math.abs(d1 - d2) / sum) :: acc
              else acc
          }
      }

      // Root mean square
      val rms = Math.sqrt(diff.map(Math.pow(_, 2)).sum / diff.length)
      Some(rms)
    }
  }

  /**
   * Compute data used for comparison between structures
   */
  def getCompData(s: Structure): ComparisonData = {
    val unitCell = new UnitCell(s.struct.lattice)
    val combinations = (s.struct.sites combinations 2).toSeq

    // Generating the list of distances (up to some maximum) between atoms,
    // one for each pair of species
    val dists = combinations flatMap { case Seq(i, j) => getCompData(i, j, unitCell) }

    // Group distances by pair of species and sort them
    dists groupBy (_._1) map { case (k, v) => (k, v.flatMap(_._2).sorted) }
  }

  /**
   * Compute the distances between two atoms
   */
  private def getCompData(s1: Site, s2: Site, unitCell: UnitCell): Seq[((Species, Species), Seq[Double])] = {
    val cutoff = unitCell.cutoff
    val a = DenseVector(s1.abc.toArray)
    val b = DenseVector(s2.abc.toArray)

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
      testDist = Math.sqrt(testDistSq)
      if testDistSq < cutoffSq
    } yield {
        testDist
    }

    val results = (resultStream take MAX_VALUES).toSeq

    // One for each pair of species (A~A, A~B, B~B)
    for {
      spec1 <- s1.species
      spec2 <- s2.species
      if spec1.element <= spec2.element
    } yield (spec1, spec2) -> results
  }

}
