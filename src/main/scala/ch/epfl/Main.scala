package ch.epfl

import ch.epfl.comparison.{Comparator, Comparison}
import ch.epfl.structure.{NaturalStructureParser, StructureParser}
import org.apache.spark.SparkContext.rddToPairRDDFunctions
import org.apache.spark.rdd.PairRDDFunctions
import org.apache.spark.{SparkConf, SparkContext}

object Main {

  /**
   * Run as follow:
   *
   * spark-submit
   *  --num-executors 100
   *  --executor-memory 10g
   *  --master yarn-cluster
   *  structure-analysis-assembly-1.0.jar
   *
   * This setting should be used as a guide but will
   * by no means be acceptable in all applications.
   * For instance, some applications might need more than
   * 10g of memory per executor.
   */
  def main(args: Array[String]) {

    val naturalsFile = "/projects/aiida/natural_structures.json"
    val syntheticsFile = "/projects/aiida/structures.json"

    findSimilar(naturalsFile, syntheticsFile, "/projects/aiida/similars")

    //findDuplicate(syntheticsFile, "/projects/aiida/duplicates")
  }

  /**
   * A spark job which finds all the pair of natural and synthetic
   * structures which are similar.
   *
   * Results are saved in a text file as a list of pair of natural structure id
   * and a list of similar synthetic structure ids: (String, List[String])
   *
   * FIXME: This job produces no results
   *
   * @param naturalsFile    File name for the natural structures
   * @param syntheticsFile  File name for the synthetic structures
   * @param outputFile      File name for output file
   */
  def findSimilar(naturalsFile: String, syntheticsFile: String, outputFile: String): Unit = {
    val conf = new SparkConf()
      .setAppName("Finding Similar Structures")
    val sc = new SparkContext(conf)

    val naturals = sc.textFile(naturalsFile)
      .flatMap(NaturalStructureParser.parse)
      .filter(_.nbElements <= 2)
      .flatMap(Comparison.renameSpecies)
      .map(s => (s.prettyFormula, s))

    val synthetics = sc.textFile(syntheticsFile)
      .flatMap(StructureParser.parse)
      .map(s => (s.prettyFormula, s))

    val similars = new PairRDDFunctions(naturals).join(synthetics)
      .map(_._2)
      .filter { case (n, s) => Comparator.areSimilar(n.scaled, s.scaled) }
      .groupByKey()
      .collect { case (n, ss) if ss.nonEmpty => (n.id, ss.map(_.id).toList) }


    similars saveAsTextFile outputFile
  }

  /**
   * A spark job which finds all the similar synthetic structures.
   *
   * Results are saved in a text file as a list of pair of structure id
   * and a list of similar structure ids: (String, List[String])
   *
   * @param structuresFile  File name for the synthetic structures
   * @param outputFile      File name for output file
   */
  def findDuplicate(structuresFile: String, outputFile: String): Unit = {
    val conf = new SparkConf()
      .setAppName("Finding Duplicate Structures")
    val sc = new SparkContext(conf)

    val structures = sc.textFile(structuresFile)
      .flatMap(StructureParser.parse)
      .map(_.scaled)
      .map(s => (s.prettyFormula, s))

    val duplicates = new PairRDDFunctions(structures).join(structures)
      .map(_._2)
      .filter { case (s1, s2) => Comparator.areSimilar(s1, s2) }
      .groupByKey()
      .collect { case (n, ss) if ss.nonEmpty => (n.id, ss.map(_.id).toList) }

    duplicates saveAsTextFile outputFile
  }

}
