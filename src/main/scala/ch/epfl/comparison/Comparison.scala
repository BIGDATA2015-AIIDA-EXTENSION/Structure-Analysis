package ch.epfl.comparison

import ch.epfl.structure._
import breeze.linalg._

object Comparator {

  val maxCellMultiples = 256
  val maxValues = 10000
  val cutoffFactor = 1.001

  implicit def lattice2UnitCell(lattice: Lattice): UnitCell =
    UnitCell(DenseVector(lattice.matrix(0).toArray), DenseVector(lattice.matrix(1).toArray), DenseVector(lattice.matrix(2).toArray), lattice.volume)

  case class UnitCell(a: DenseVector[Double], b: DenseVector[Double], c: DenseVector[Double], volume: Double) {
    val aCrossB = cross(a, b)
    val aCrossBLen = Math.sqrt(aCrossB dot aCrossB)
    val aCrossBHat = aCrossB / aCrossBLen

    val bCrossC = cross(b, c);
    val bCrossCLen = Math.sqrt(bCrossC dot bCrossC)
    val bCrossCHat = bCrossC / bCrossCLen

    val aCrossC = cross(a, c)
    val aCrossCLen = Math.sqrt(aCrossC dot aCrossC)
    val aCrossCHat = aCrossC / aCrossCLen;

    val cutoff = max(a.norm, b.norm, c.norm)
  }

  def distance(s1: Structure, s2: Structure): Double = {
    val unitCell1 = lattice2UnitCell(s1.struct.lattice)
    val ds1 = distances(s1, unitCell = s1.struct.lattice, maxCellMultiples = maxCellMultiples, cutoff = unitCell1.cutoff, maxValues = maxValues)

    val unitCell2 = lattice2UnitCell(s2.struct.lattice)
    val ds2 = distances(s2, unitCell = s2.struct.lattice, maxCellMultiples = maxCellMultiples, cutoff = unitCell2.cutoff, maxValues = maxValues)

    // Not sure how to compute the difference between two set of list.
    // What order should be used, what about set/list of different size...
    val diff = ???

    // TODO Compute the root mean square of the diff
    ???
  }

  /**
   * Given a structure return a Map from a pair of atoms to a sorted sequence of distances
   */
  def distances(s: Structure, unitCell: UnitCell, maxCellMultiples: Int, cutoff: Double, maxValues: Int): Map[(Site, Site), Seq[Double]] = {
    val combinations = s.struct.sites combinations 2
    val dists = combinations map { case Seq(i, j) => (i, j) -> distances(i, j, unitCell, maxCellMultiples, cutoff, maxValues).sorted }
    dists.toMap
  }

  /**
   * Compute the distances between two atoms
   */
  def distances(s1: Site, s2: Site, unitCell: UnitCell, maxCellMultiples: Int, cutoff: Double, maxValues: Int): Seq[Double] = {
    val a = DenseVector(s1.abc.toArray)
    val b = DenseVector(s2.abc.toArray)

    val dR = b - a

    // Got rid of the `problemDuringCalculation`, seems to be discarded in the original implementation
    val aMax: Int = (Math.floor(getNumPlaneRepetitionsToBoundSphere(cutoff + Math.abs(dR dot unitCell.bCrossCHat), unitCell.volume, unitCell.bCrossCLen)) max maxCellMultiples).toInt
    val bMax: Int = (Math.floor(getNumPlaneRepetitionsToBoundSphere(cutoff + Math.abs(dR dot unitCell.aCrossCHat), unitCell.volume, unitCell.aCrossCLen)) max maxCellMultiples).toInt
    val cMax: Int = (Math.floor(getNumPlaneRepetitionsToBoundSphere(cutoff + Math.abs(dR dot unitCell.aCrossBHat), unitCell.volume, unitCell.aCrossBLen)) max maxCellMultiples).toInt

    val cutoffSq = cutoff * cutoff

    val results =
      for {
        a <- -aMax to aMax
        rA = a.toDouble * unitCell.a
        b <- -bMax to bMax
        rAB = rA + b.toDouble * unitCell.b
        c <- -cMax to cMax
        outVec = rAB + c.toDouble * unitCell.c + dR
        testDistSq = outVec dot outVec
        testDist = Math.sqrt(testDistSq) if testDistSq < cutoffSq
      } yield {
        testDist
      }

    if (results.length < maxValues) results
    else Seq()
  }

  def getNumPlaneRepetitionsToBoundSphere(radius: Double, volume: Double, crossLen: Double): Double = {
    radius / volume * crossLen
  }

}
