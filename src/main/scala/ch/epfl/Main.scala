package ch.epfl

import ch.epfl.comparison.{Comparator, Comparison}
import ch.epfl.structure.{NaturalStructureParser, StructureParser}
import org.apache.spark.SparkContext.rddToPairRDDFunctions
import org.apache.spark.rdd.PairRDDFunctions
import org.apache.spark.{SparkConf, SparkContext}

object Main {

  /**
   * This job produces no result.
   * TODO: Find potential issue
   *
   * Run as follow:
   *
   * spark-submit
   *  --num-executors 100
   *  --executor-memory 10g
   *  --master yarn-cluster
   *  structure-analysis-assembly-1.0.jar
   *
   * Yes Martin!! We can join big RDDs but we obviously need more than 512MB of memory!!
   */
  def main(args: Array[String]) {
    val conf = new SparkConf() setAppName "Structure Analysis"
    val sc = new SparkContext(conf)

    val naturals =
      sc.textFile("/projects/aiida/natural_structures.json")
        .flatMap(NaturalStructureParser.parse)
        .filter(_.nbElements <= 2)
        .flatMap(Comparison.renameSpecies)
        .map(s => (s.prettyFormula, s))

    val synthetics =
      sc.textFile("/projects/aiida/structures.json")
        .flatMap(StructureParser.parse)
        .map(s => (s.prettyFormula, s))


    val similar =
      (new PairRDDFunctions(naturals) join synthetics)
        .map(_._2)
        .filter { case (n, s) => Comparator.areSimilar(n.scaled, s.scaled) }
        .groupByKey()


    similar collect {
      case (n, ss) if ss.nonEmpty => (n.id, ss.map(_.id).toList)
    } saveAsTextFile "/projects/aiida/similars"
  }

}
