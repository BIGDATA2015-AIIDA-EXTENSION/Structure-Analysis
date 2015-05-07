package ch.epfl.clustering

/**
 * Created by lukas on 07/05/15.
 */
object PlottingFormatter {
  def toPlot[T](clusterings: ClusteredStructure[T], toVector: (T) => Vector[Double]): String = {
    toPlot(clusterings :: Nil, Nil, toVector)
  }

  def toPlot[T](clusterings: List[ClusteredStructure[T]], metric: ClusterMetric, toVector: (T) => Vector[Double]): String = {
    toPlot(clusterings, metric :: Nil, toVector)
  }
  
  def toPlot[T](clusterings: List[ClusteredStructure[T]], metrics: List[ClusterMetric], toVector: (T) => Vector[Double]): String = {
    ???
  }
}

case class ClusterMetric(name: String, values: List[(Double, Double)])
