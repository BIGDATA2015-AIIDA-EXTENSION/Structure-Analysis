package ch.epfl.clustering

import play.api.libs.json.Json

/**
 * Created by lukas on 07/05/15.
 */
object PlottingFormatter {
  def toPlot[T](clusterings: ClusteredStructure[T], info: Option[String], toVector: (T) => Vector[Double]): String = {
    toPlot(clusterings :: Nil, info, Nil, toVector)
  }

  def toPlot[T](clusterings: List[ClusteredStructure[T]], info: Option[String], metric: ClusterMetric, toVector: (T) => Vector[Double]): String = {
    toPlot(clusterings, info, metric :: Nil, toVector)
  }

  private case class Point(x: Double, y: Double)
  private case class Metric(name: String, values: List[Point])
  private case class Output(id: String, info: String, clusterings: List[List[List[Vector[Double]]]], metrics: List[Metric])
  
  def toPlot[T](clusterings: List[ClusteredStructure[T]], info: Option[String], metrics: List[ClusterMetric], toVector: (T) => Vector[Double]): String = {



    implicit val pointWrites = Json.writes[PlottingFormatter.Point]
    implicit val metricWrites = Json.writes[PlottingFormatter.Metric]
    implicit val outputWrites = Json.writes[PlottingFormatter.Output]

    val out = Output(
      clusterings.head.id,
      info.getOrElse(""),
      clusterings.map(_.clusters.map(cl => cl.elems.map(toVector))),
      metrics.map(m => Metric(m.name, m.values.map(p => Point(p._1, p._2))))
    )

    Json.toJson(out).toString()

  }
}

case class ClusterMetric(name: String, values: List[(Double, Double)])
