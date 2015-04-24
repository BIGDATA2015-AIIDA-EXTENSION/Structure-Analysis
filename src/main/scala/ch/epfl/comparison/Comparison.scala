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

    val cutoff = cutoffFactor * max(a.norm, b.norm, c.norm)
  }

  def distance(s1: Structure, s2: Structure): Option[Double] = {

    if (s1.nbElements != s2.nbElements) None
    else if ((s1.elements.toSet intersect s2.elements.toSet).isEmpty) None
    else {
      val ds1 = distances(s1)
      val ds2 = distances(s2)

      val sum =
        ds1 map {
          case (k, _vs1) =>
            val _vs2 = ds2(k)
            val min = _vs1.size min _vs2.size

            val vs1 = _vs1 take min
            val vs2 = _vs2 take min

            ((vs1 zip vs2) foldLeft 0.0) {
              case (acc, (d1, d2)) =>
                val sum = d1 + d2
                val delta = if (sum > 0) Math.abs(d1 - d2) / sum else 0
                delta * delta
            }
        }

      Some(Math.sqrt(sum.sum))
    }
  }

  /**
   * Given a structure return a Map from a pair of atoms to a sorted sequence of distances
   */
  def distances(s: Structure): Map[(Species, Species), Seq[Double]] = {

    val unitCell = lattice2UnitCell(s.struct.lattice)
    val combinations = (s.struct.sites combinations 2).toSeq

    combinations flatMap {
      case Seq(i, j) => distances(i, j, unitCell)
    } groupBy (_._1) map { case (k, v) => (k, v.map(_._2).flatten.sorted) }

  }

  /**
   * Compute the distances between two atoms
   */
  def distances(s1: Site, s2: Site, unitCell: UnitCell): Seq[((Species, Species), Seq[Double])] = {
    val cutoff = unitCell.cutoff
    val a = DenseVector(s1.abc.toArray)
    val b = DenseVector(s2.abc.toArray)

    val dR = b - a

    // Got rid of the `problemDuringCalculation`, seems to be discarded in the original implementation
    val aMax: Int = (Math.floor(getNumPlaneRepetitionsToBoundSphere(cutoff + Math.abs(dR dot unitCell.bCrossCHat), unitCell.volume, unitCell.bCrossCLen)) max maxCellMultiples).toInt
    val bMax: Int = (Math.floor(getNumPlaneRepetitionsToBoundSphere(cutoff + Math.abs(dR dot unitCell.aCrossCHat), unitCell.volume, unitCell.aCrossCLen)) max maxCellMultiples).toInt
    val cMax: Int = (Math.floor(getNumPlaneRepetitionsToBoundSphere(cutoff + Math.abs(dR dot unitCell.aCrossBHat), unitCell.volume, unitCell.aCrossBLen)) max maxCellMultiples).toInt

    val cutoffSq = cutoff * cutoff

    val results = {
      val tmpResults =
        for {
            a <- (-aMax to aMax).toStream
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

      (tmpResults take maxValues).toSeq
    }

    for {
      spec1 <- s1.species
      spec2 <- s2.species if spec1.element <= spec2.element
    } yield (spec1, spec2) -> results


  }

  def getNumPlaneRepetitionsToBoundSphere(radius: Double, volume: Double, crossLen: Double): Double = {
    radius / volume * crossLen
  }

}
