package ch.epfl.clustering

import ch.epfl.structure.{Structure, StructureParserIvano}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.JavaConverters._

/**
 * Created by lukas on 27/04/15.
 */
object ClusterStruct {

  val k = 1

  def compute(args: Array[String]) = {
    val sc = new SparkContext(new SparkConf().setAppName("AiidaComputations"))

    val jsonStructures = sc.textFile("hdfs://" + args(0))

    val parsed = jsonStructures flatMap StructureParserIvano.parse

    val parsedStruct = parsed.map(Structure.convertIvano).cache()
    parsedStruct.groupBy(_.struct.sites.size).map{ case (n, iter) => (n, iter.size) }.sortBy(_._1, ascending = false).saveAsTextFile("hdfs://" + args(1))

    //val plotCluster = dataForPlottingClusters(parsedStruct)
    //val plotMetrics = dataForPlottingMetrics(parsed)
    //val plotPlaneMetrics = dataForMetrics(10, parsed)


    //plotCluster.saveAsTextFile("hdfs://" + args(1)+"plot-cluster")
    //plotMetrics.saveAsTextFile("hdfs://" + args(1)+"plot-clusterNumberMetrics")
    //plotPlaneMetrics.saveAsTextFile("hdfs://" + args(1)+"plot-plane-metrics")

    sc.stop()
  }

  def dataForPlottingClusters(rdd: RDD[Structure]): RDD[String] = {
    rdd.map{s =>
      val nbClusters = bestClusterNb(s)
      new HierarchicalClustering(toPointList(s, k)).compute(nbClusters).clusters().asScala.map(_.toString).mkString(":")
    }
  }

  def bestClusterNb(s: Structure): Int = {

      val nbAtoms = k*k*k*s.struct.sites.size

      val nbClusterWithMetric = (nbAtoms/2 to nbAtoms) map {i =>
        val cluster = new HierarchicalClustering(toPointList(s, k))
        (i, cluster.compute(i).clusterNumberMetrics())
      }
    nbClusterWithMetric.maxBy(_._2)._1
  }

  def dataForPlottingMetrics(rdd: RDD[Structure]): RDD[(String, String)] = {
    rdd.map{s =>
      (s.id, (1 to toPointList(s, k).size() map {i =>
        val cluster = new HierarchicalClustering(toPointList(s, k))
        cluster.compute(i).clusterNumberMetrics()
      }).mkString(":"))
    }
  }

  def dataForMetrics(nbClusters: Int, rdd: RDD[Structure]): RDD[(String, String)] = {
    rdd.map(s => (s.id, new HierarchicalClustering(toPointList(s, k)).compute(k*k*k).metrics().toString))
  }

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
