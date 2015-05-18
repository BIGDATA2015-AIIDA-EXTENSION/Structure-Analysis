package ch.epfl.comparison

import breeze.linalg.{DenseMatrix, DenseVector}
import ch.epfl.structure._
import org.apache.spark.SparkContext.rddToPairRDDFunctions
import org.apache.spark.{SparkConf, SparkContext}

/**
 * This object contains a collection of Spark jobs
 * to do computations on structures,
 * as well as some helper functions which modify structures.
 *
 * You can run a spark job as follow:
 *
 * spark-submit
 *  --num-executors 100
 *  --executor-memory 10g
 *  --master yarn-cluster
 *  structure-analysis-assembly-1.0.jar
 *
 * These settings should be used as a guide but will
 * by no means be acceptable for all Spark job.
 * For instance, some applications might need more than
 * 10g of memory per executor.
 */
object Comparison {

  /**
   * A spark job which finds all the pair of natural and synthetic
   * structures which are similar.
   *
   * Results are saved in a text file as a list of pair of natural structure id
   * and a list of similar synthetic structure ids: (String, List[String])
   *
   * Pros:
   *  - Uses less memory
   *  - Join produces less results (than cartesian product)
   *
   * Cons:
   *  - Redundant computations
   *  - Unequaled repartition across the nodes after the join
   *
   * @param naturalsFile    File name for the natural structures
   * @param syntheticsFile  File name for the synthetic structures
   * @param outputFile      File name for output file
   */
  def findSimilar1(naturalsFile: String, syntheticsFile: String, outputFile: String): Unit = {
    val conf = new SparkConf()
      .setAppName("Finding Similar Structures")
    val sc = new SparkContext(conf)

    // Parse natural structures
    // Keep the ones with less than 2 elements
    // Normalize structures
    // Rename elements
    // Create key for the join
    val naturals = sc.textFile(naturalsFile)
      .flatMap(NaturalStructureParser.parse)
      .filter(_.nbElements <= 2)
      .map(normalize)
      .flatMap(renameSpecies)
      .map(s => (s.prettyFormula, s))

    // Parse synthetic structures
    // Normalize structures
    // Rename elements
    // Create key for the join
    val synthetics = sc.textFile(syntheticsFile)
      .flatMap(StructureParser.parse)
      .map(normalize)
      .flatMap(renameSpecies)
      .map(s => (s.prettyFormula, s))

    // Join between naturals and synthetics on pretty formula
    // Get rid of the join key
    // Keep only the pair of similar ones
    // Group all the pair with the same natural structure
    // Keep only the ids
    val similars = naturals.join(synthetics)
      .map(_._2)
      .filter { case (n, s) => Comparator.areSimilar(n, s) }
      .groupByKey()
      .map { case (n, ss) => (n.id, ss.map(_.id).toList) }

    similars saveAsTextFile outputFile
  }

  /**
   * A spark job which finds all the pair of natural and synthetic
   * structures which are similar.
   *
   * Results are saved in a text file as a list of pair of natural structure id
   * and a list of similar synthetic structure ids: (String, List[String])
   *
   * Pros:
   *  - No redundant computations
   *  - Join produces less results (than cartesian product)
   *
   * Cons:
   *  - Uses more memory
   *  - Unequaled repartition across the nodes after the join
   *
   * @param naturalsFile    File name for the natural structures
   * @param syntheticsFile  File name for the synthetic structures
   * @param outputFile      File name for output file
   */
  def findSimilar2(naturalsFile: String, syntheticsFile: String, outputFile: String): Unit = {
    val conf = new SparkConf()
      .setAppName("Finding Similar Structures")
    val sc = new SparkContext(conf)

    // Parse natural structures
    // Keep the ones with less than 2 elements
    // Normalize structures
    // Rename elements
    // Create key for the join and get comparison data
    val naturals = sc.textFile(naturalsFile)
      .flatMap(NaturalStructureParser.parse)
      .filter(_.nbElements <= 2)
      .map(normalize)
      .flatMap(renameSpecies)
      .map(s => (s.prettyFormula, (s.id, Comparator.getCompData(s))))

    // Parse synthetic structures
    // Normalize structures
    // Rename elements
    // Create key for the join and get comparison data
    val synthetics = sc.textFile(syntheticsFile)
      .flatMap(StructureParser.parse)
      .map(normalize)
      .flatMap(renameSpecies)
      .map(s => (s.prettyFormula, (s.id, Comparator.getCompData(s))))

    // Join between naturals and synthetics on pretty formula
    // Get rid of the join key
    // Keep only the pair of similar ones
    // Keep only the ids
    // Group all the pair with the same natural structure
    val similars = naturals.join(synthetics)
      .map(_._2)
      .filter { case (n, s) => Comparator.areSimilar(n._2, s._2) }
      .map { case (n, s) => (n._1, s._1) }
      .groupByKey()
      .map { case (n, ss) => (n, ss.toList) }

    similars saveAsTextFile outputFile
  }

  /**
   * A spark job which finds all the pair of natural and synthetic
   * structures which are similar.
   *
   * Results are saved in a text file as a list of pair of natural structure id
   * and a list of similar synthetic structure ids: (String, List[String])
   *
   * Pros:
   *  - Good repartition across the nodes after the cartesian product
   *
   * Cons:
   *  - Redundant computations
   *
   * @param naturalsFile    File name for the natural structures
   * @param syntheticsFile  File name for the synthetic structures
   * @param outputFile      File name for output file
   */
  def findSimilar3(naturalsFile: String, syntheticsFile: String, outputFile: String): Unit = {
    val conf = new SparkConf()
      .setAppName("Finding Similar Structures")
    val sc = new SparkContext(conf)

    // Parse natural structures
    // Keep the ones with less than 2 elements
    // Normalize structures
    // Rename elements
    val naturals = sc.textFile(naturalsFile)
      .flatMap(NaturalStructureParser.parse)
      .filter(_.nbElements <= 2)
      .map(normalize)
      .flatMap(renameSpecies)

    // Parse synthetic structures
    // Normalize structures
    // Rename elements
    val synthetics = sc.textFile(syntheticsFile)
      .flatMap(StructureParser.parse)
      .map(normalize)
      .flatMap(renameSpecies)

    // Cartesian product between naturals and synthetics
    // Keep only the pair of similar ones
    // Keep only the ids
    // Group all the pair with the same natural structure
    val similars = naturals.cartesian(synthetics)
      .filter { case (n, s) => Comparator.areSimilar(n, s) }
      .map { case (n, s) => (n.id, s.id) }
      .groupByKey()
      .map { case (n, ss) => (n, ss.toList) }

    similars saveAsTextFile outputFile
  }

  /**
   * A spark job which finds all the similar synthetic structures.
   *
   * Results are saved in a text file as a list of pair of structure id
   * and a list of similar structure ids: (String, List[String])
   *
   * Cons:
   *  - Very inefficient
   *
   * @param structuresFile  File name for the synthetic structures
   * @param outputFile      File name for output file
   */
  def findDuplicate1(structuresFile: String, outputFile: String): Unit = {
    val conf = new SparkConf()
      .setAppName("Finding Duplicate Structures")
    val sc = new SparkContext(conf)

    // Parse synthetic structures
    // Normalize structures
    // Rename elements
    // Key for the join
    val structures = sc.textFile(structuresFile)
      .flatMap(StructureParser.parse)
      .map(normalize)
      .flatMap(renameSpecies)
      .map(s => (s.prettyFormula, s))

    // Join between synthetics and themselves on pretty formula
    // Get rid of the join key
    // Keep only the pair of similar ones
    // Group all the pair with the same synthetic structure
    // Keep only the ids
    val duplicates = structures.join(structures)
      .map(_._2)
      .filter { case (s1, s2) => Comparator.areSimilar(s1, s2) }
      .groupByKey()
      .map { case (n, ss) => (n.id, ss.map(_.id).toList) }

    duplicates saveAsTextFile outputFile
  }

  /**
   * A spark job which finds all the similar synthetic structures.
   *
   * Results are saved in a text file as a list of pair of structure id
   * and a list of similar structure ids: (String, List[String])
   *
   * Pros:
   *  - Much more efficient
   *
   * Cons:
   *  - Might produce false positives and false negatives
   *
   * @param structuresFile  File name for the synthetic structures
   * @param outputFile      File name for output file
   */
  def findDuplicate2(structuresFile: String, outputFile: String): Unit = {
    val conf = new SparkConf()
      .setAppName("Finding Duplicate Structures")
    val sc = new SparkContext(conf)

    // Parse synthetic structures
    // Normalize structures
    // Rename elements
    val structures = sc.textFile(structuresFile)
      .flatMap(StructureParser.parse)
      .map(normalize)
      .flatMap(renameSpecies)

    // Zero element before reduce phase
    // Reduce phase
    // Keep only the structures with duplicates
    val duplicates = structures
      .map(s => (s.prettyFormula, Map(s -> List[String]())))
      .reduceByKey(merge)
      .flatMap ( _._2 collect { case (s, ds) if ds.nonEmpty => (s.id, ds) })

    duplicates saveAsTextFile outputFile
  }

  type Duplicates = Map[Structure, List[String]]

  /**
   * @return a merge of two set of duplicates
   */
  def merge(dups1: Duplicates, dups2: Duplicates): Duplicates = {
    if (dups1.isEmpty) dups2
    else if (dups2.isEmpty) dups1
    else {
      val head @ (s1, s1milar) = dups1.head
      val sim = dups2 find (s2 => Comparator areSimilar(s1, s2._1))
      sim match {
        case Some((s2, s2milar)) =>
          merge(dups1.tail, dups2 - s2) + (s1 -> (s2.id :: s1milar ::: s2milar))
        case None =>
          merge(dups1.tail, dups2) + head
      }
    }
  }

  val alphabet = List("A", "B")

  /**
   * @return a structure with its species renamed according to the alphabet
   */
  def renameSpecies(structure: Structure): List[Structure] = {
    require(structure.nbElements <= alphabet.length)
    val newElems = alphabet take structure.nbElements
    val elemSet = newElems.toSet

    structure.elements.toList.permutations.toList map { oldElems =>
      val substitutions = (oldElems zip newElems).toMap

      val sites = structure.struct.sites map { site =>
        val newSpecies = site.species map { s =>
          s.copy(element = substitutions(s.element))
        }
        site.copy(species = newSpecies)
      }

      val prettyFormula = (newElems map { e =>
        val count = sites count (_.species.exists(_.element == e))
        if (count > 1) s"e$count"
        else if (count == 1) e
        else ""
      }).mkString

      structure.copy(elements = elemSet,
        struct = structure.struct.copy(sites = sites),
        prettyFormula = prettyFormula)
    }
  }

  /**
   * @return a structure with its lattice and sites normalized
   */
  def normalize(structure: Structure): Structure = {
    val Struct(sites, lattice) = structure.struct
    val factor = Math.cbrt(structure.nbSites / lattice.volume)

    val normLattice = normalizeLattice(lattice, factor)

    val normMatrix = DenseMatrix.tabulate(3, 3) { case (i, j) =>
      normLattice.matrix(i)(j)
    }

    val normSites = sites map { s =>
      val xyz = normMatrix * DenseVector(s.abc.toArray)
      s.copy(xyz = xyz.toArray.toList)
    }

    val normStruct = Struct(normSites, normLattice)
    structure.copy(struct = normStruct)
  }

  private def normalizeLattice(lattice: Lattice, factor: Double): Lattice = {
    val a = lattice.a * factor
    val b = lattice.b * factor
    val c = lattice.c * factor
    val matrix = lattice.matrix map (_ map (_ * factor))

    lattice.copy(a = a, b = b, c = c, matrix = matrix, volume = a * b * c)
  }
}
