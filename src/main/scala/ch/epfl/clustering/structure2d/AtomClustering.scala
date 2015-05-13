package ch.epfl.clustering.structure2d

import ch.epfl.clustering.{ClusterMetric, Clustering, PlottingFormatter, ClusteredStructure}
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

  def subst(a: Vector[Double], b: Vector[Double]): Vector[Double] = {
    a.zip(b).map(cpl => cpl._1 - cpl._2)
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
            val listPositions = elems.flatMap(elm => elems.map(e => subst(elm.position, e.position)))
            Helpers.computeRank(listPositions)
        }.max
    }.max
  }

  def computeMetric2(clusteredStructure: ClusteredStructure[Atom]): Double = {
    clusteredStructure.clusters.map {
      cluster =>
        val atoms = cluster.elems.groupBy(atom => atom.id)
        val mean = atoms.map {
          case (id, elems) =>
            val listDist = elems.flatMap(elm => elems.map(e => subst(elm.position, e.position)))

            Helpers.computeRank(listDist)
        }.sum / atoms.size
        mean * cluster.elems.size
    }.sum.toDouble / clusteredStructure.clusters.foldLeft(0.0){case (sum, cluster) => sum + cluster.elems.size}
  }

  def regressionMetric(clusteredStructure: ClusteredStructure[Atom]): Double = {
    val meanErrors = clusteredStructure.clusters.map(cluster => Helpers.regressionError[Atom](_.position)(cluster))
    meanErrors.max
  }

  def clusterSizeMetric(clusteredStructure: ClusteredStructure[Atom]): Double = {
    clusteredStructure.clusters.map(_.elems.size).min
  }

  def computeClusters(struct: Structure, inflation: Int = 3): String = {
    val atoms = atomsFromStructure(struct, 3)
    val maxClusterNumber = Math.ceil(Math.sqrt(atoms.length/2)).toInt
    val clusterings = Clustering.cluster(struct.id, atoms, distance _, 1 to maxClusterNumber)
    val metric1 = clusterings.map(computeMetric(_).toDouble)
    val metric2 = clusterings.map(computeMetric2)
    val metric3 = clusterings.map(clusterSizeMetric)
    val metric4 = clusterings.map(regressionMetric)

    val is2d = metric1.zip(metric3).map{ case (rank, minCluster) => rank <= 2 && minCluster >= inflation }.count(identity) >= 2


    val metric1WithIndex = metric1.zipWithIndex.map{ case (v, i) => ((i+1).toDouble, v) }
    val metric2WithIndex = metric2.zipWithIndex.map{ case (v, i) => ((i+1).toDouble, v) }
    val metric3WithIndex = metric3.zipWithIndex.map{ case (v, i) => ((i+1).toDouble, v) }
    val metric4WithIndex = metric4.zipWithIndex.map{ case (v, i) => ((i+1).toDouble, v) }

    PlottingFormatter.toPlot(
      clusterings,
      Some(is2d.toString()),
      List(
        ClusterMetric("Max", metric1WithIndex),
        ClusterMetric("Mean", metric2WithIndex),
        ClusterMetric("MinCluster", metric3WithIndex),
        ClusterMetric("Regression", metric4WithIndex)
      ),
      (a:Atom) => a.position)
  }



  def multiCLuster(s: Structure, inflation: Int): List[(Int, Int, ClusteredStructure[Atom])] = {
    val bigStructure = atomsFromStructure(s, inflation)
    val maxClusters = Math.ceil(Math.sqrt(bigStructure.length) / 2).toInt
    (1 until (maxClusters + 1)).map {
      nb =>
        val clusteredStructure = Clustering.cluster[Atom](s.id, bigStructure, distance _, nb)
        val metric = computeMetric(clusteredStructure)
        (nb, metric, clusteredStructure)
    }.toList
  }

  def is2D(struct: Structure, inflation: Int): Boolean = {
    val atoms = atomsFromStructure(struct, inflation)
    val maxClusterNumber = Math.ceil(Math.sqrt(atoms.length/2)).toInt
    val clusterings = Clustering.cluster(struct.id, atoms, distance _, 1 to maxClusterNumber)
    val metric1 = clusterings.map(computeMetric)
    val metric2 = clusterings.map(clusterSizeMetric)
    metric1.zip(metric2).map{ case (rank, minCluster) => rank <= 2 && minCluster >= inflation }.count(identity) >= 2
  }

  def compute(args: Array[String]) = {
    val sc = new SparkContext(new SparkConf().setAppName("AiidaComputations"))

    val jsonStructures = sc.textFile("hdfs://" + args(0))

    val parsed = jsonStructures flatMap StructureParserIvano.parse
    val parsedStruct = parsed.map(Structure.convertIvano).cache()

    val plotCluster = parsedStruct map(computeClusters(_, 3))
    plotCluster.saveAsTextFile("hdfs://" + args(1))
    sc.stop()
  }

/*
  def compute2d(args: Array[String]) = {
    val sc = new SparkContext(new SparkConf().setAppName("AiidaComputations"))

    val jsonStructures = sc.textFile("hdfs://" + args(0))

    val parsed = jsonStructures flatMap StructureParserIvano.parse
    val parsedStruct = parsed.map(Structure.convertIvano).cache()

    val plotCluster = parsedStruct.map(s => (s.id, is2D(s, 3)))
    plotCluster.saveAsTextFile("hdfs://" + args(1))
    sc.stop()
  }*/

}