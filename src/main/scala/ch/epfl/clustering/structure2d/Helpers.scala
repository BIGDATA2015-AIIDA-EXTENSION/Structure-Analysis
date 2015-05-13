package ch.epfl.clustering.structure2d

import breeze.linalg._
import ch.epfl.clustering.Cluster
import scala.collection.immutable.Vector

object Helpers {

  def computeRank(list: List[Vector[Double]]) = {
    if (list.exists(l => l.length != 3)) {
      throw new IllegalArgumentException("Vectors should have three components.")
    }
    val matrix = matrixFromList(list)
    rank(matrix)
  }

  def matrixFromList(list: List[Vector[Double]]): DenseMatrix[Double] = DenseMatrix(list.map(_.toArray):_*)

  def regressionError[T](vectorFromT: T => Vector[Double])(cluster: Cluster[T]): Double = {
    def findLargestEntry(m: DenseMatrix[Double]): Double = {
      m.flatten().toArray.map(Math.abs).max
    }

    def findEigenVectorAssociatedWithLargestEigenValue(m: DenseMatrix[Double]): DenseVector[Double] = {
      val scale = findLargestEntry(m)
      val scaledMatrix: DenseMatrix[Double] = m * scale
      val mc1: DenseMatrix[Double] = scaledMatrix*scaledMatrix
      val mc2: DenseMatrix[Double] = mc1 * mc1
      val mc3: DenseMatrix[Double] = mc2 * mc2

      var v:DenseVector[Double] = DenseVector(Array[Double](1.0, 1.0, 1.0))

      0 until 100 foreach {
        i =>
          val intermediate: DenseVector[Double] = mc3 * v
          v = intermediate/norm(intermediate)
      }

      v
    }

    def norm(v: DenseVector[Double]): Double = {
      Math.sqrt(v dot v)
    }

    def solve( choleskyMatrix: DenseMatrix[Double], b: DenseVector[Double] ) : DenseVector[Double] = {
      val C = choleskyMatrix
      val size = C.rows
      if( C.rows != C.cols ) {
        // throw exception or something
      }
      if( b.length != size ) {
        // throw exception or something
      }
      // first we solve C * y = b
      // (then we will solve C.t * x = y)
      val y = DenseVector.zeros[Double](size)
      // now we just work our way down from the top of the lower triangular matrix
      for( i <- 0 until size ) {
        var sum = 0.0
        for( j <- 0 until i ) {
          sum += C(i,j) * y(j)
        }
        y(i) = ( b(i) - sum ) / C(i,i)
      }
      // now calculate x
      val x = DenseVector.zeros[Double](size)
      val Ct = C.t
      // work up from bottom this time
      for( i <- size -1 to 0 by -1 ) {
        var sum = 0.0
        for( j <- i + 1 until size ) {
          sum += Ct(i,j) * x(j)
        }
        x(i) = ( y(i) - sum ) / Ct(i,i)
      }
      x
    }

    def findLLSQPlane(vectors: List[DenseVector[Double]]): (DenseVector[Double], DenseVector[Double]) = {
      val count = vectors.length
      val sum: DenseVector[Double] = vectors.foldLeft(DenseVector.zeros[Double](3)){case (acc, v) => acc + v}
      val center: DenseVector[Double] = sum * (1.0/count)
      var sumXX=0.0
      var sumXY=0.0
      var sumXZ=0.0
      var sumYY=0.0
      var sumYZ=0.0
      var sumZZ=0.0
      vectors.foreach {
        v =>
          val diffX = v(0) - center(0)
          val diffY = v(1) - center(1)
          val diffZ = v(2) - center(2)
          sumXX+=diffX*diffX
          sumXY+=diffX*diffY
          sumXZ+=diffX*diffZ
          sumYY+=diffY*diffY
          sumYZ+=diffY*diffZ
          sumZZ+=diffZ*diffZ
      }
      val v1 = Vector[Double](sumXX,sumXY,sumXZ)
      val v2 = Vector[Double](sumXY,sumYY,sumYZ)
      val v3 = Vector[Double](sumXZ,sumYZ,sumZZ)
      val m: DenseMatrix[Double] = matrixFromList(List(v1, v2, v3))
      val determinant = det(m)
      if (determinant == 0) {
        (center, solve(cholesky(m), DenseVector.zeros[Double](3)))
      } else {
        (center, findEigenVectorAssociatedWithLargestEigenValue(inv(m)))
      }
    }

    val vectors = cluster.elems.map(el => DenseVector[Double](vectorFromT(el).toArray))
    if (vectors.length >= 3) {
      val (c, n) = findLLSQPlane(vectors)
      val d = (-1) * (n(0) * c(0) + n(1) * c(1) + n(2) * c(2))
      val errors: List[Double] = vectors.map {
        v =>
          Math.abs(n(0) * v(0) + n(1) * v(1) + n(2) * v(2) + d) / norm(n)
      }
      val totError = errors.foldLeft(0.0)(_ + _)
      totError / errors.length
    } else 0.0
    /*def sum(vectors: List[Vector[Double]])(extract: Vector[Double] => Double): Double = {
      vectors.foldLeft(0.0) { case (acc, v) => acc + extract(v) }
    }
    val vectors = cluster.elems.map(vectorFromT)
    if (vectors.size >= 3) {
      println(vectors.size)
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
      val ata = A * A
      val atb = A * b
      val invata = inv(ata)
      val reg: DenseVector[Double] = invata * atb
      //val reg = A \ b
      val n = DenseVector(Array[Double](reg(0), reg(1), -1))
      val c = reg(2)


      val errors: List[Double] = vectors.map {
        v =>
          val p = DenseVector(v.toArray)
          Math.abs((n dot p) + c) / Math.sqrt(n dot n)
      }
      val totError = errors.foldLeft(0.0)(_ + _)
      totError / errors.length
    } else 0.0*/
  }

}
