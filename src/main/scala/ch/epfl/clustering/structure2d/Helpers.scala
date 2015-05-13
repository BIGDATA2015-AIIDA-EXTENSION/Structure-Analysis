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

    /*def findEigenVectorAssociatedWithLargestEigenValue(m: DenseMatrix[Double]): DenseVector[Double] = {
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
    } else 0.0*/
    def sum(vectors: List[Vector[Double]])(extract: Vector[Double] => Double): Double = {
      vectors.foldLeft(0.0) { case (acc, v) => acc + extract(v) }
    }

    /*def jacobiMethod(a: DenseMatrix[Double], b: DenseVector[Double]): DenseVector[Double] = {

      def maxDiagonal(a: DenseMatrix[Double], b: DenseVector[Double]): (DenseMatrix[Double], DenseVector[Double]) = {
        println("#"*20)
        println(a)
        println(b)
        println("-"*20)
        val ((x, y, z), _) = (for{
          x <- 0 to 2
          y <- 0 to 2
          z <- 0 to 2
          if x != y && x != z && y != z
        } yield ((x, y, z), a(x, 0)+a(y, 1)+a(z, 2))).maxBy(_._2)

        val new_b = DenseVector[Double](Array[Double](b(x), b(y), b(z)))
        val col1 = Vector(a(x, 0), a(y, 0), a(z, 0))
        val col2 = Vector(a(x, 1), a(y, 1), a(z, 1))
        val col3 = Vector(a(x, 2), a(y, 2), a(z, 2))
        val new_a = matrixFromList(List(col1, col2, col3))
        println(new_a)
        println(new_b)
        println("#"*20)

        (new_a, new_b)
      }

      def helper(funcs: List[DenseVector[Double] => Double])(iter: Int, input: DenseVector[Double]): DenseVector[Double] = {

        def isEqual(x: Double, y: Double, z: Double, input: DenseVector[Double]): Boolean = {
          val error = 0.001
          Math.abs(x - input(0)) < error &&
          Math.abs(y - input(1)) < error &&
          Math.abs(z - input(2)) < error
        }
        val maxIter = 10

        val l @ List(x, y, z) = funcs.map(_(input))
        println(l)
        if(isEqual(x, y, z, input) || iter == maxIter) {
          println(iter)
          input
        }
        else helper(funcs)(iter+1,DenseVector[Double](Array[Double](x, y, z)))
      }
      val sol = DenseVector[Double](Array[Double](0.0, 0.0, 0.0))

      val (a_new, b_new) = maxDiagonal(a, b)

      def f1(d: DenseVector[Double]): Double = {
        (b_new(0) + a_new(0, 1)*d(1) + a_new(0, 2)*d(2))/a_new(0, 0)
      }

      def f2(d: DenseVector[Double]): Double = {
        (b_new(1) + a_new(1, 0)*d(0)+a_new(1,2)*d(2))/a_new(1,1)
      }

      def f3(d: DenseVector[Double]): Double = {
        (b_new(2) + a_new(2, 0)*d(0) + a_new(2, 1)*d(1))/a_new(2,2)
      }

      helper(List(f1, f2, f3))(0, sol)
    }*/

    val vectors = cluster.elems.map(vectorFromT)
    if (vectors.size >= 3) {
      //println(vectors.size)
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

        //println("det 0")
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
      //println(reg)
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
    } else 0.0
  }

}
