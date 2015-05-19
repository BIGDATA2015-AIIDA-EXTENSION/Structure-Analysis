package ch.epfl.clustering.structure2d

import breeze.linalg._
import ch.epfl.clustering.Cluster

import scala.collection.immutable.Vector

object Helpers {

  /**
   * Computes the rank of the matrix given by the list of vector
   *
   * @param list
   * @return
   */
  def computeRank(list: List[Vector[Double]]) = {
    if (list.exists(l => l.length != 3)) {
      throw new IllegalArgumentException("Vectors should have three components.")
    }
    val matrix = matrixFromList(list)
    rank(matrix)
  }

  def matrixFromList(list: List[Vector[Double]]): DenseMatrix[Double] = DenseMatrix(list.map(_.toArray):_*)

  /**
   * Computes the mean distance between every point and the plane that minimizes the sum of square distances between this plane and the points
   *
   * @param vectorFromT
   * @param cluster
   * @tparam T
   * @return
   */
  def regressionError[T](vectorFromT: T => Vector[Double])(cluster: Cluster[T]): Double = {

    def sum(vectors: List[Vector[Double]])(extract: Vector[Double] => Double): Double = {
      vectors.foldLeft(0.0) { case (acc, v) => acc + extract(v) }
    }


    val vectors = cluster.elems.map(vectorFromT)
    if (vectors.size >= 3) {
      val sumXSq = sum(vectors)(v => v(0) * v(0))
      val sumYSq = sum(vectors)(v => v(1) * v(1))
      val sumXY = sum(vectors)(v => v(0) * v(1))
      val sumXZ = sum(vectors)(v => v(0) * v(2))
      val sumYZ = sum(vectors)(v => v(1) * v(2))
      val sumX = sum(vectors)(v => v(0))
      val sumY = sum(vectors)(v => v(1))
      val sumZ = sum(vectors)(v => v(2))

      val col1 = Vector[Double](sumXSq, sumXY, sumX)
      val col2 = Vector[Double](sumXY, sumYSq, sumY)
      val col3 = Vector[Double](sumX, sumY, vectors.length)
      val b = DenseVector(Array(sumXZ, sumYZ, sumZ))
      val A = matrixFromList(List(col1, col2, col3))


      def plusDelta(a: DenseMatrix[Double], i: Int): DenseMatrix[Double] = {
        val identity = DenseMatrix.eye[Double](3)
        (identity :* Math.pow(10, -i)) + a
      }

      val reg: DenseVector[Double] = if(det(A) == 0) {

        val trans = A.t
        val ata = trans * A
        val _ata = plusDelta(ata, 2)
        if(det(_ata) == 0) {
          DenseVector[Double](Array(0.0, 0.0, 0.0))
        } else {
          val inverse = inv(_ata)
          val atab = trans * b
          inverse * atab
        }

      } else {
        A \ b
      }

      val n = DenseVector(Array[Double](reg(0), reg(1), -1))
      val c = reg(2)


      val errors: List[Double] = vectors.map {
        v =>
          val p = DenseVector(v.toArray)
          Math.abs((n dot p) + c) / Math.sqrt(n dot n)
      }
      val totError = errors.foldLeft(0.0)(_ + _)
      totError / errors.length
    } else 0.0
  }

}
