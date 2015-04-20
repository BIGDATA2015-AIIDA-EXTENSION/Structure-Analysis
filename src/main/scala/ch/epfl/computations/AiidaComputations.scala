package ch.epfl.computations

import ch.epfl.structure.{Params, Structure, StructureParser}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

object AiidaComputations {

  def compute(args: Array[String]) {
    val sc = new SparkContext(new SparkConf().setAppName("AiidaComputations"))

    val jsonStructures = sc.textFile("hdfs://" + args(0))

    val parsed = jsonStructures flatMap StructureParser.parse

    parsed.cache()

    val mapViewer = new MapViewer(List(AAEpsilon(1), ABEpsilon(1.5)), ABSigma, BBEpsilon)

    val groups = mapViewer.getMap(parsed)

    groups.saveAsTextFile("hdfs://" + args(1))
    sc.stop()
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


trait MapAxis {
  def getValue(structure: Structure): Double
}
case object AASigma extends MapAxis {
  override def getValue(structure: Structure): Double = structure.potential.params.aa.sigma
}
case object AAEpsilon extends MapAxis {
  override def getValue(structure: Structure): Double = structure.potential.params.aa.epsilon
}
case object ABSigma extends MapAxis {
  override def getValue(structure: Structure): Double = structure.potential.params.ab.sigma
}
case object ABEpsilon extends MapAxis {
  override def getValue(structure: Structure): Double = structure.potential.params.ab.epsilon
}
case object BBSigma extends MapAxis {
  override def getValue(structure: Structure): Double = structure.potential.params.bb.sigma
}
case object BBEpsilon extends MapAxis {
  override def getValue(structure: Structure): Double = structure.potential.params.bb.epsilon
}




case class MapViewer(conds: List[Condition], mapAxisX: MapAxis, mapAxisY: MapAxis) {
  private def getFilteredResults(rdd: RDD[Structure]): RDD[Structure] = {

    def applyCond(conds: List[Condition], rdd: RDD[Structure]): RDD[Structure] = conds match {
      case Nil => rdd
      case cond :: tail => applyCond(tail, rdd.filter(s => cond.condition(s.potential.params)))
    }

    applyCond(conds, rdd)
  }

  def getMap(rdd: RDD[Structure]): RDD[((Double, Double), Structure)] = {

    def groupCond(structure: Structure) = (mapAxisX.getValue(structure), mapAxisY.getValue(structure))

    getFilteredResults(rdd).groupBy(groupCond).map{ case (key, value) => (key, value.minBy(_.energyPerSite)) }
  }

}