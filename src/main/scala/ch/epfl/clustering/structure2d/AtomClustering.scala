package ch.epfl.clustering.structure2d

import ch.epfl.clustering.{ClusterMetric, ClusteredStructure, Clustering, PlottingFormatter}
import ch.epfl.structure.{NaturalStructureParser, Structure}
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
    val maxClusterNumber = Math.ceil(Math.sqrt(atoms.length/2)).toInt
    val clusterings = Clustering.cluster(struct.id, atoms, distance _, 1 to maxClusterNumber)

    //Here we apply the different metrics to our clusterings
    val rankMetric = clusterings.map(computeRankMetric(_).toDouble)
    val clusterSizeMetric = clusterings.map(computeClusterSizeMetric)
    val planeMetric = clusterings.map(computeRegressionMetric)

    // We zip the metrics with indexes to be able to plot them
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

  def compute(args: Array[String]) = {
    val MAX_ATOMS_IN_STRUCTURE = 40


    val sc = new SparkContext(new SparkConf().setAppName("AiidaComputations"))

    //Reads the input file line by line
    val jsonStructures = sc.textFile("hdfs://" + args(0))


    // Sends the data across executors
    val structs: RDD[String] = jsonStructures.repartition(sc.getExecutorMemoryStatus.size)

    //Here we parse the ivano structures and convert them to the structure case class we defined.
    val parsedStruct = structs flatMap NaturalStructureParser.parse

    //We filter the structures to keep only the structures with 'MAX_ATOMS_IN_STRUCTURE' or less and apply the clustering
    val plotCluster = parsedStruct.filter(_.struct.sites.size <= MAX_ATOMS_IN_STRUCTURE).map(computeClusters(_, 3))

    // Finally we save the result to a file

    plotCluster.saveAsTextFile("hdfs://" + args(1))
    sc.stop()
  }


}