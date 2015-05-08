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

  private case class Point(x: Double, y: Double)
  private case class Metric(name: String, values: List[Point])
  private case class Output(clusterings: List[List[List[Vector[Double]]]], metrics: List[Metric])
  
  def toPlot[T](clusterings: List[ClusteredStructure[T]], metrics: List[ClusterMetric], toVector: (T) => Vector[Double]): String = {



    implicit val pointWrites = Json.writes[PlottingFormatter.Point]
    implicit val metricWrites = Json.writes[PlottingFormatter.Metric]
    implicit val outputWrites = Json.writes[PlottingFormatter.Output]
    val out = Output(
      clusterings.map(_.clusters.map(cl => cl.elems.map(toVector))),
      metrics.map(m => Metric(m.name, m.values.map(p => Point(p._1, p._2)))))
    Json.toJson(out).toString

  }
}

case class ClusterMetric(name: String, values: List[(Double, Double)])
