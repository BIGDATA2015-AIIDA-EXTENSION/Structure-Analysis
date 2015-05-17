package ch.epfl

import ch.epfl.comparison.Comparison._

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
}
