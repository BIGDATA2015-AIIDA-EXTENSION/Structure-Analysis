package ch.epfl.clustering.structure2d

import ch.epfl.clustering.{ClusterMetric, Clustering, PlottingFormatter, ClusteredStructure}
import ch.epfl.structure.{Structure, StructureParserIvano}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

/**
 *
 *
 * This object is an example of how to use the clustering.
 * In this example we clusterise the atoms of a structure by using the euclidean distance as distance function.
 *
 * We use several metrics to find the optimal number of cluster:
 *  - The minimal number of elements in a cluster
 *  - The maximum rank of the distances between periodically repeated atoms inside a cluster
 *  - The mean distance from any atom in a cluster to the plane that minimizes the sum of the distances to the plane.
 *
 */


object AtomClustering {

  /**
   * This is the class that represents a element of the clustering. It has a position and an id.
   * The id is used to know from which atom it was replicated.
   *
   * @param id
   * @param position
   */
  class Atom(val id: Int, val position: Vector[Double]) {
    require(position.length == 3)
  }

  /**
   * This function transforms a Structure into a list of Atoms.
   *
   * @param s the structure to transform
   * @param k the number of replications in all directions.
   * @return the list of atoms that represents the structure
   */
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

  /**
   * Distance function between two Atom.
   * Returns the euclidean distance between the two Atoms.
   *
   */
  def distance(elm1: Atom, elm2: Atom): Double = {
    val sqrSum = elm1.position.zip(elm2.position).foldLeft(0.0) {
      case (acc, (p1, p2)) => acc + Math.pow(p1 - p2, 2)
    }
    Math.sqrt(sqrSum)
  }

  def add(a: Vector[Double], b: Vector[Double]): Vector[Double] = {
    a.zip(b).map(cpl => cpl._1 + cpl._2)
  }

  def sub(a: Vector[Double], b: Vector[Double]): Vector[Double] = {
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

  def computeRankMetric(clusteredStructure: ClusteredStructure[Atom]): Int = {
    //For each cluster we compute the rank for each atom id.
    //We then take the max of the max.
    clusteredStructure.clusters.map {
      cluster =>
        //groups by atom id (To get only repeated atoms)
        cluster.elems.groupBy(atom => atom.id).map {
          case (id, elems) =>
            //Computes the distances between all atoms
            val listPositions = elems.flatMap(elm => elems.map(e => sub(elm.position, e.position)))
            Helpers.computeRank(listPositions)
        }.max
    }.max
  }

  def computeRegressionMetric(clusteredStructure: ClusteredStructure[Atom]): Double = {
    //We apply the plane regression to all clusters and take the maximal error.
    val meanErrors = clusteredStructure.clusters.map(cluster => Helpers.regressionError[Atom](_.position)(cluster))
    meanErrors.max
  }

  def computeClusterSizeMetric(clusteredStructure: ClusteredStructure[Atom]): Double = {
    // Gets the number of elements in each cluster in we keep the minimal value.
    clusteredStructure.clusters.map(_.elems.size).min
  }

  /**
   * This function does all the work for a given structure.
   * - It transforms the structure into a list of Atoms
   * - Does the clustering for a range of clusterings
   * - Computes the metrics on the earlier computed clusterings
   *
   * @param struct
   * @param inflation
   * @return
   */
  def computeClusters(struct: Structure, inflation: Int = 3): String = {
    val atoms = atomsFromStructure(struct, inflation)
    println(atoms.size)
    val maxClusterNumber = Math.ceil(Math.sqrt(atoms.length/2)).toInt
    val clusterings = Clustering.cluster(struct.id, atoms, distance _, 1 to maxClusterNumber)
    
    val rankMetric = clusterings.map(computeRankMetric(_).toDouble)
    val clusterSizeMetric = clusterings.map(computeClusterSizeMetric)
    val planeMetric = clusterings.map(computeRegressionMetric)


    val rankMetricWithIndex = rankMetric.zipWithIndex.map{ case (v, i) => ((i+1).toDouble, v) }
    val clusterSizeMetricWithIndex = clusterSizeMetric.zipWithIndex.map{ case (v, i) => ((i+1).toDouble, v) }
    val planeMetricWithIndex = planeMetric.zipWithIndex.map{ case (v, i) => ((i+1).toDouble, v) }

    PlottingFormatter.toPlot(
      clusterings,
      Some(planeMetricWithIndex.zip(clusterSizeMetric).filter(_._2 >= inflation).minBy(_._1._2)._1._1.toString),
      List(
        ClusterMetric("Max rank", rankMetricWithIndex),
        ClusterMetric("MinCluster", clusterSizeMetricWithIndex),
        ClusterMetric("Regression", planeMetricWithIndex)
      ),
      (a:Atom) => a.position)
  }



  def multiCLuster(s: Structure, inflation: Int): List[(Int, Int, ClusteredStructure[Atom])] = {
    val bigStructure = atomsFromStructure(s, inflation)
    val maxClusters = Math.ceil(Math.sqrt(bigStructure.length) / 2).toInt
    (1 until (maxClusters + 1)).map {
      nb =>
        val clusteredStructure = Clustering.cluster[Atom](s.id, bigStructure, distance _, nb)
        val metric = computeRankMetric(clusteredStructure)
        (nb, metric, clusteredStructure)
    }.toList
  }

  def compute(args: Array[String]) = {
    val sc = new SparkContext(new SparkConf().setAppName("AiidaComputations"))

    val jsonStructures = sc.textFile("hdfs://" + args(0))

    val structs: RDD[String] = jsonStructures.repartition(sc.getExecutorMemoryStatus.size)
    val parsed = structs flatMap StructureParserIvano.parse
    val parsedStruct = parsed.map(Structure.convertIvano).cache()

    val plotCluster = parsedStruct map(computeClusters(_, 3))
    plotCluster.saveAsTextFile(args(1))
    sc.stop()
  }


}