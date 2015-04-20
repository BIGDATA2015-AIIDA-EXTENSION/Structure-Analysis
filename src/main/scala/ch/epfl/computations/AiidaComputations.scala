package ch.epfl.computations

import ch.epfl.structure.{Struct, StructureParser}
import org.apache.spark.{SparkConf, SparkContext}

object AiidaComputations {

  def compute(args: Array[String]) {
    val sc = new SparkContext(new SparkConf().setAppName("AiidaComputations"))

    val jsonStructures = sc.textFile("hdfs://" + args(0))

    val parsed = jsonStructures flatMap StructureParser.parse

    parsed.cache()

    val groupByEnergy = parsed.groupBy(struct => Math.ceil(struct.energy / 5) * 5)
    val groupByPressure = parsed.groupBy(struct => Math.ceil(struct.pressure / 0.5e-5) * 0.5e-5)
    val groupBySimilarities = parsed.groupBy(rStr => rStr.unitCellFormula).map(couple => couple._2).flatMap(
      l => l.map(
        elm => l.filter(inelm => areSimilar(elm.struct, inelm.struct, 1))
      )
    )

    groupByEnergy.saveAsTextFile(args(1) + "/energy")
    groupByPressure.saveAsTextFile(args(1) + "/pressure")
    groupBySimilarities.saveAsTextFile(args(1) + "/similarities")

    sc.stop()
  }

  def distance(xyz1: Seq[Double], xyz2: Seq[Double]): Double = {
    // Pairs the coordinates axis by axis
    val pairs = xyz1.zip(xyz2)
    // Sums the squares of the difference of the coordinates (which is the squared distance)
    val sumOfSquares = pairs.foldLeft(0.0)((acc, cpl) => acc + (cpl._1 - cpl._2) * (cpl._1 - cpl._2))
    // Actual distance
    Math.sqrt(sumOfSquares)
  }

  def areSimilar(struct1: Struct, struct2: Struct, errorMargin: Double): Boolean = {
    // We assume that they have the same unitCellFormula
    val sites1 = struct1.sites
    val sites2 = struct2.sites

    sites1.forall(elm1 => sites2.exists(elm2 => distance(elm1.xyz, elm2.xyz) < errorMargin))
  }
}