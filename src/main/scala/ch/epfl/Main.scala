package ch.epfl

import ch.epfl.comparison.Comparator
import ch.epfl.structure.StructureParser
import org.apache.spark.{SparkConf, SparkContext}

import scala.io.Source

object Main {
  def main(args: Array[String]) {
    val fileName = "/projects/aiida/structures.json"
    val conf = new SparkConf() setAppName "Structure Analysis"
    val sc = new SparkContext(conf)

    val lines = sc textFile fileName
    val structs = lines flatMap StructureParser.parse
    val ids = Set(
      "54b87493a0530f2245e7a9a1",
      "54f582b401162b54f25762ae",
      "54b87492a0530f2245e7a995",
      "54b87491a0530f2245e7a982",
      "53f52acc1c371006e1138f00",
      "54b8748ea0530f2245e7a955",
      "54b8748ea0530f2245e7a94f",
      "53f52ac91c371006e1138e73",
      "53f52acc1c371006e1138f05"
    )

    val filtered = structs filter (ids contains _.id)
    val toCompare = filtered.collect().toList

    println(toCompare map (_.id))

    // FIXME  thread "Driver" scala.MatchError: java.lang.NoSuchMethodError: breeze.linalg.package$.InjectNumericOps(Ljava/lang/Object;)Ljava/lang/Object; (of class java.lang.NoSuchMethodError) at org.apache.sp
    val distances = (toCompare combinations 2) map {
      case List(s1, s2) => (s1.id, s2.id) -> Comparator.distance(s1, s2)
    }

    distances.toList foreach println
  }

  // def main(args: Array[String]) {
  //   val fileName = getClass getResource "/structures.json"
  //   val source = Source fromURL fileName

  //   val structs = source getLines() flatMap StructureParser.parse
  //   val comparables = structs.toList groupBy (s => (s.nbSites, s.prettyFormula)) map (_._2) filter (_.length > 2)

  //   comparables flatMap { cs =>
  //     cs combinations 2 take 10 map {
  //       case List(s1, s2) => (s1.id, s2.id) -> Comparator.distance(s1, s2)
  //     }
  //   }
  // }
}
