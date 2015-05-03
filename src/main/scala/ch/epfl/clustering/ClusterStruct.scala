package ch.epfl.clustering

import ch.epfl.structure.{Structure, StructureParserIvano}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.JavaConverters._

/**
 * Created by lukas on 27/04/15.
 */
object ClusterStruct {

  val k = 3

  def compute(args: Array[String]) = {
    val sc = new SparkContext(new SparkConf().setAppName("AiidaComputations"))

    val jsonStructures = sc.textFile("hdfs://" + args(0))

    val parsed = jsonStructures flatMap StructureParserIvano.parse

    val parsedStruct = parsed.map(Structure.convertIvano).cache()

    val plotCluster = dataForPlottingClusters(parsedStruct, true, true)


    plotCluster.saveAsTextFile("hdfs://" + args(1))

    sc.stop()
  }

  def dataForPlottingClusters(rdd: RDD[Structure], withClusterNumberMetrics:Boolean = false, genAllPlots: Boolean = false): RDD[String] = {
    rdd.map{s =>
      val clusterings = clusterForAllValues(s)
      val clusterNumber = clusterMetrics(clusterings)
      val planeMetrics = planeMetric(clusterings).map(_._2).mkString(" ")
      val clustersAsString = clusterings.map{ case (_, cl) => cl.clusters().asScala.map(_.toString).mkString(":") }.mkString(";")
      if(withClusterNumberMetrics) {
        s"$clustersAsString|${clusterNumber.map(_._2).mkString(" ")}:$planeMetrics"
      } else {
        clustersAsString
      }
    }
  }

  def clusterForAllValues(s: Structure): Seq[(Int, Clustering)] = {
    val nbAtoms = k*k*k*s.struct.sites.size

    (1 to Math.sqrt(nbAtoms/2.0).toInt) map {i =>
      val cluster = new HierarchicalClustering(toPointList(s, k))
      (i, cluster.compute(i))
    }
  }

  def planeMetric(clusterings: Seq[(Int, Clustering)]): Seq[(Int, Double)] = {
    clusterings.map{ case (nbCluster, clustering) => (nbCluster, clustering.metrics()) }
  }


  def clusterMetrics(clusterings: Seq[(Int, Clustering)]): Seq[(Int, Double)] = {
    clusterings.map{ case (nbCluster, clustering) => (nbCluster, clustering.clusterNumberMetrics()) }
  }

  def bestClusterNb(values: Seq[(Int, Double)]): Int = values.maxBy(_._2)._1


  def toPointList(s: Structure, k: Int): java.util.List[Vector] = {
    s.struct.sites.flatMap{ site =>
      val Seq(x, y, z) = site.xyz

      val axisX = new Vector(s.struct.lattice.a, 0.0, 0.0)
      val axisY = new Vector(0.0, s.struct.lattice.b, 0.0)
      val axisZ = new Vector(0.0, 0.0, s.struct.lattice.c)

      for {
        i <- 0 until k
        j <- 0 until k
        l <- 0 until k
      } yield new Vector(x, y, z).add(axisX, i).add(axisY, j).add(axisZ, l)


    }.asJava
  }
}
