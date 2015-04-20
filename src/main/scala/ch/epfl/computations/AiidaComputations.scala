package ch.epfl.computations

import ch.epfl.structure.{Params, Structure, StructureParser, Param}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

object AiidaComputations {

  def compute(args: Array[String]) {
    val sc = new SparkContext(new SparkConf().setAppName("AiidaComputations"))

    val jsonStructures = sc.textFile("hdfs://" + args(0))

    val parsed = jsonStructures flatMap StructureParser.parse

    parsed.cache()


    val mapViewer = new MapViewer(List((0.4, 1.0), (0.6, 1.0), (0.8, 1.0), (1.0, 1.0), (1.2, 1.0), (1.4, 1.0), (1.6, 1.0), (1.1, 1.0)), (ABSigma, BBSigma), ABEpsilon, BBEpsilon)

    val groups = mapViewer.getMap(parsed)

    groups.saveAsTextFile("hdfs://"+ args(1))

    sc.stop()
  }


}


trait MapAxis
case object AASigma extends MapAxis
case object AAEpsilon extends MapAxis
case object ABSigma extends MapAxis
case object ABEpsilon extends MapAxis
case object BBSigma extends MapAxis
case object BBEpsilon extends MapAxis


case class MapID(cond1: Double, cond2: Double, anonymousFormula: String, param : Param)

case class MapElement(id: MapID, x: Double, y: Double, value: Int)

case class MapViewer(conds: List[(Double, Double)], mapConds: (MapAxis, MapAxis), mapAxisX: MapAxis, mapAxisY: MapAxis) {

  private def getAxisValue(mapAxis: MapAxis): Params => Double = mapAxis match {
    case AASigma => p: Params => p.aa.sigma
    case AAEpsilon => p: Params => p.aa.epsilon
    case BBSigma => p: Params => p.bb.sigma
    case BBEpsilon => p: Params => p.bb.epsilon
    case ABSigma => p: Params => p.ab.sigma
    case ABEpsilon => p: Params => p.ab.epsilon
  }

  private def condition(mapAxis: MapAxis, value: Double):Params => Boolean = {
    p: Params => getAxisValue(mapAxis)(p) == value
  }


  private def getFilteredResults(rdd: RDD[Structure]): RDD[Structure] = {


    def combinedCondition(conds: List[(Double, Double)]): Structure => Boolean = conds match {
      case Nil => sys.error("error")
      case (c1, c2) :: Nil => s:Structure => condition(mapConds._1, c1)(s.potential.params) && condition(mapConds._2, c2)(s.potential.params)
      case (c1, c2) :: tail => s:Structure => (condition(mapConds._1, c1)(s.potential.params) && condition(mapConds._2, c2)(s.potential.params)) | combinedCondition(tail)(s)
    }
    rdd.filter(combinedCondition(conds))
  }


  def getMap(rdd: RDD[Structure]): RDD[(MapID, List[(Double, Double, Int)])] = {

    def groupCond(structure: Structure) = {
      val p = structure.potential.params
      (getAxisValue(mapConds._1)(p), getAxisValue(mapConds._2)(p), structure.anonymousFormula, getAxisValue(mapAxisX)(p), getAxisValue(mapAxisY)(p), structure.potential.params.aa)
    }

    val mapElems = getFilteredResults(rdd).groupBy(groupCond).map{ case (key, value) => MapElement(MapID(key._1, key._2, key._3, key._6), key._4, key._5, value.minBy(_.energyPerSite).spaceGroup.number) }
    mapElems.groupBy(_.id).map{ case (k, v) => (k, v.toList.map(me => (me.x, me.y, me.value)))}

  }

}