package ch.epfl.clustering

import play.api.libs.json.Json

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
    val clusteringsAsJson = Json.toJson(clusterings.map(_.clusters.map(cl => cl.elems.map(toVector))))

    implicit val metricWrites = Json.writes[ClusterMetric]
    val metricsAsJson = Json.toJson(metrics)
    val valuesAsMap = ("clusterings" -> clusterings, "metrics" -> metricsAsJson)
    Json.toJson(valuesAsMap).toString
  }
}

case class ClusterMetric(name: String, values: List[(Double, Double)])
