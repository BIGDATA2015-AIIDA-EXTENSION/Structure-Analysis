package ch.epfl.clustering

import ch.epfl.structure.{Structure, StructureParserIvano}
import org.apache.spark.{SparkConf, SparkContext}

object AtomClustering {

  class Atom(val id: Int, val position: Vector[Double]) {
    require(position.length == 3)
  }

  def atomsFromStructure(s: Structure, k: Int): List[Atom] = {
    s.struct.sites.zipWithIndex.flatMap {
      case (site, index) =>
        val Seq(x, y, z) = site.xyz

        val origin = Vector[Double](x, y, z)

        val axisX = Vector[Double](s.struct.lattice.a, 0.0, 0.0)
        val axisY = Vector[Double](0.0, s.struct.lattice.b, 0.0)
        val axisZ = Vector[Double](0.0, 0.0, s.struct.lattice.c)

        for {
          i <- 0 until k
          j <- 0 until k
          l <- 0 until k
        } yield new Atom(index, sum(List(origin, mult(axisX, i), mult(axisY, j), mult(axisZ, l))))
    }.toList
  }

  def distance(elm1: Atom, elm2: Atom): Double = {
    val sqrSum = elm1.position.zip(elm2.position).foldLeft(0.0) {
      case (acc, (p1, p2)) => acc + Math.pow(p1 - p2, 2)
    }
    Math.sqrt(sqrSum)
  }

  def add(a: Vector[Double], b: Vector[Double]): Vector[Double] = {
    a.zip(b).map(cpl => cpl._1 + cpl._2)
  }

  def mult(a: Vector[Double], factor: Double): Vector[Double] = {
    a.map(e => factor * e)
  }

  def sum(list: List[Vector[Double]]): Vector[Double] = {
    require(list.forall(vect => vect.length == 3))
    list.foldLeft(Vector[Double](0.0, 0.0, 0.0)) {
      case (acc, vec) =>
        add(acc, vec)
    }
  }

  def computeMetric(clusteredStructure: ClusteredStructure[Atom]): Int = {
    clusteredStructure.clusters.map {
      cluster =>
        cluster.elems.groupBy(atom => atom.id).map {
          case (id, elems) =>
            val listPositions = elems.map(elm => elm.position)
            helpers.computeRank(listPositions)
        }.max
    }.max
  }

  def multiCLuster(s: Structure, inflation: Int): List[(Int, Int, ClusteredStructure[Atom])] = {
    val bigStructure = atomsFromStructure(s, inflation)
    val maxClusters = Math.ceil(Math.sqrt(bigStructure.length) / 2).toInt
    (1 until (maxClusters + 1)).map {
      nb =>
        val clusteredStructure = Clustering.cluster[Atom](bigStructure, distance _, nb)
        val metric = computeMetric(clusteredStructure)
        (nb, metric, clusteredStructure)
    }.toList
  }

  def compute(args: Array[String]) = {
    val sc = new SparkContext(new SparkConf().setAppName("AiidaComputations"))

    val jsonStructures = sc.textFile("hdfs://" + args(0))

    val parsed = jsonStructures flatMap StructureParserIvano.parse
    val parsedStruct = parsed.map(Structure.convertIvano).cache()

    val plotCluster = parsedStruct.map(s => multiCLuster(s, 3))
    plotCluster.saveAsTextFile("hdfs://" + args(1))
    sc.stop()
  }

}