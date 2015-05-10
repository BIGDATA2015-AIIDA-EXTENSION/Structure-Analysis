package ch.epfl.clustering.structure2d

import breeze.linalg.{DenseMatrix, rank}

object Helpers {

  def computeRank(list: List[Vector[Double]]) = {
    if (list.exists(l => l.length != 3)) {
      throw new IllegalArgumentException("Vectors should have three components.")
    }
    val matrix = matrixFromList(list)
    rank(matrix)
  }

  def matrixFromList(list: List[Vector[Double]]): DenseMatrix[Double] = DenseMatrix(list.map(_.toArray):_*)
}
