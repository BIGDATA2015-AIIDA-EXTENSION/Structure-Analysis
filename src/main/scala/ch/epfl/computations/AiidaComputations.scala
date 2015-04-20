package ch.epfl.computations

import ch.epfl.structure.{Params, Structure, Struct, StructureParser}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

object AiidaComputations {

  def compute(args: Array[String]) {
    val sc = new SparkContext(new SparkConf().setAppName("AiidaComputations"))

    val jsonStructures = sc.textFile("hdfs://" + args(0))

    val parsed = jsonStructures flatMap StructureParser.parse

    parsed.cache()

    val mapViewer = new MapViewer(List(AAEpsilon(1), ABEpsilon(1.5)))

    val filteredSorted = mapViewer.getSortedResults(parsed)

    println("######## "+filteredSorted.count())

    filteredSorted.saveAsTextFile("hdfs:///user/lukas/project/output/")
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

trait Condition {
  def condition(p: Params): Boolean
}

case class AASigma(value: Double) extends Condition {
  override def condition(p: Params): Boolean = p.aa.sigma == value
}
case class AAEpsilon(value: Double) extends Condition {
  override def condition(p: Params): Boolean = p.aa.epsilon == value
}
case class BBSigma(value: Double) extends Condition {
  override def condition(p: Params): Boolean = p.bb.sigma == value
}
case class BBEpsilon(value: Double) extends Condition {
  override def condition(p: Params): Boolean = p.bb.epsilon == value
}
case class ABSigma(value: Double) extends Condition {
  override def condition(p: Params): Boolean = p.ab.sigma == value
}
case class ABEpsilon(value: Double) extends Condition {
  override def condition(p: Params): Boolean = p.ab.epsilon == value
}


class MapViewer(conds: List[Condition]) {
  def getSortedResults(rdd: RDD[Structure]): RDD[Double] = {

    def applyCond(conds: List[Condition], rdd: RDD[Structure]): RDD[Structure] = conds match {
      case Nil => rdd
      case cond :: tail => applyCond(tail, rdd.filter(s => cond.condition(s.potential.params)))
    }

    applyCond(conds, rdd).sortBy(s => s.energy).map(s => s.energy)
  }
}