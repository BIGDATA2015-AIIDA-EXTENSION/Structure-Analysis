package ch.epfl.computations

import ch.epfl.structure._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

object AiidaComputations {

  def compute(args: Array[String]) {
    val sc = new SparkContext(new SparkConf().setAppName("AiidaComputations"))

    val jsonStructures = sc.textFile("hdfs://" + args(0))
    val pairs = jsonStructures.map(s => (s,1))
    val counts = pairs.reduceByKey((a, b) => a + b)

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


case class MapID(cond1: Double, cond2: Double, prettyFormula: String)

case class MapElement(id: MapID, x: Double, y: Double, value: Int)

case class MapViewer(conds: List[(Double, Double)], mapConds: (MapAxis, MapAxis), mapAxisX: MapAxis, mapAxisY: MapAxis) {

  private def getAxisValue(mapAxis: MapAxis, p: Params): Double = mapAxis match {
    case AASigma => p.aa.sigma
    case AAEpsilon => p.aa.epsilon
    case BBSigma => p.bb.sigma
    case BBEpsilon => p.bb.epsilon
    case ABSigma => p.ab.sigma
    case ABEpsilon => p.ab.epsilon
  }

  private def condition(mapAxis: MapAxis, value: Double, p: Params): Boolean = {
    getAxisValue(mapAxis, p) == value
  }


  private def getFilteredResults(rdd: RDD[Structure]): RDD[Structure] = {


    def combinedCondition(conds: List[(Double, Double)]): Structure => Boolean = conds match {
      case Nil => sys.error("error")
      case (c1, c2) :: Nil => s:Structure => condition(mapConds._1, c1, s.potential.params) && condition(mapConds._2, c2, s.potential.params)
      case (c1, c2) :: tail => s:Structure => (condition(mapConds._1, c1, s.potential.params) && condition(mapConds._2, c2, s.potential.params)) | combinedCondition(tail)(s)
    }
    rdd.filter(combinedCondition(conds))
  }


  def getMap(rdd: RDD[Structure]): RDD[(MapID, List[(Double, Double, Int)])] = {

    def groupCond(structure: Structure) = {
      val p = structure.potential.params
      (getAxisValue(mapConds._1, p), getAxisValue(mapConds._2, p), structure.prettyFormula, getAxisValue(mapAxisX, p), getAxisValue(mapAxisY, p))
    }

    def structToMapID(struct: Structure): MapID =
      MapID(getAxisValue(mapConds._1, struct.potential.params), getAxisValue(mapConds._2, struct.potential.params), struct.prettyFormula)

    def structToMapElement(struct: Structure): MapElement =
      MapElement(structToMapID(struct), getAxisValue(mapAxisX, struct.potential.params), getAxisValue(mapAxisY, struct.potential.params), struct.spaceGroup.number)

    val usefulStructures = getFilteredResults(rdd)
    val structuresMappedToKeyValue: RDD[(String, Structure)] = usefulStructures.map(s => (s.potential.params_id, s))

    val structuresWithMinEnergy = structuresMappedToKeyValue.reduceByKey{ case (s1, s2) => if (s1.energyPerSite < s2.energyPerSite) s1 else s2 }

    structuresWithMinEnergy.map{ case (param_id, struct) => (structToMapID(struct), structToMapElement(struct)) }
      .groupBy(_._1).map{ case (id, iter) => (id, iter.map{ case (k, v) => (v.x, v.y, v.value)}.toList) }

  }

}