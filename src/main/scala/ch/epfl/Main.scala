package ch.epfl

import ch.epfl.comparison.Comparator
import ch.epfl.structure.StructureParser
import org.apache.spark.{SparkConf, SparkContext}

import scala.io.Source

object Main {

  val reflResults: Map[(String, String), Double] =
    Map(
      ("54b87493a0530f2245e7a9a1", "54b87493a0530f2245e7a9a1") -> 0,
      ("54b87493a0530f2245e7a9a1", "54f582b401162b54f25762ae") -> 0.131696,
      ("54b87493a0530f2245e7a9a1", "54b87492a0530f2245e7a995") -> 0.301859,
      ("54b87493a0530f2245e7a9a1", "54b87491a0530f2245e7a982") -> 0.135633,
      ("54b87493a0530f2245e7a9a1", "53f52acc1c371006e1138f00") -> 0.136592,
      ("54b87493a0530f2245e7a9a1", "54b8748ea0530f2245e7a955") -> 0.187729,
      ("54b87493a0530f2245e7a9a1", "54b8748ea0530f2245e7a94f") -> 0.187729,
      ("54b87493a0530f2245e7a9a1", "53f52ac91c371006e1138e73") -> 0.124715,
      ("54b87493a0530f2245e7a9a1", "53f52acc1c371006e1138f05") -> 0.123937,
      
      ("54f582b401162b54f25762ae", "54f582b401162b54f25762ae") -> 0,
      ("54f582b401162b54f25762ae", "54b87492a0530f2245e7a995") -> 0.347932,
      ("54f582b401162b54f25762ae", "54b87491a0530f2245e7a982") -> 0.135506,
      ("54f582b401162b54f25762ae", "53f52acc1c371006e1138f00") -> 0.14179,
      ("54f582b401162b54f25762ae", "54b8748ea0530f2245e7a955") -> 0.260705,
      ("54f582b401162b54f25762ae", "54b8748ea0530f2245e7a94f") -> 0.260705,
      ("54f582b401162b54f25762ae", "53f52ac91c371006e1138e73") -> 0.127145,
      ("54f582b401162b54f25762ae", "53f52acc1c371006e1138f05") -> 0.131566,
      
      ("54b87492a0530f2245e7a995", "54b87492a0530f2245e7a995") -> 0,
      ("54b87492a0530f2245e7a995", "54b87491a0530f2245e7a982") -> 0.300061,
      ("54b87492a0530f2245e7a995", "53f52acc1c371006e1138f00") -> 0.295419,
      ("54b87492a0530f2245e7a995", "54b8748ea0530f2245e7a955") -> 0.29365,
      ("54b87492a0530f2245e7a995", "54b8748ea0530f2245e7a94f") -> 0.29365,
      ("54b87492a0530f2245e7a995", "53f52ac91c371006e1138e73") -> 0.304301,
      ("54b87492a0530f2245e7a995", "53f52acc1c371006e1138f05") -> 0.29847,
      
      ("54b87491a0530f2245e7a982", "54b87491a0530f2245e7a982") -> 0,
      ("54b87491a0530f2245e7a982", "53f52acc1c371006e1138f00") -> 0.0304518, 
      ("54b87491a0530f2245e7a982", "54b8748ea0530f2245e7a955") -> 0.0555921,
      ("54b87491a0530f2245e7a982", "54b8748ea0530f2245e7a94f") -> 0.0551285,
      ("54b87491a0530f2245e7a982", "53f52ac91c371006e1138e73") -> 0.0507691,
      ("54b87491a0530f2245e7a982", "53f52acc1c371006e1138f05") -> 0.0610846,
      
      ("53f52acc1c371006e1138f00", "53f52acc1c371006e1138f00") -> 0,
      ("53f52acc1c371006e1138f00", "54b8748ea0530f2245e7a955") -> 0.147847,
      ("53f52acc1c371006e1138f00", "54b8748ea0530f2245e7a94f") -> 0.147847,
      ("53f52acc1c371006e1138f00", "53f52ac91c371006e1138e73") -> 0.0588303, 
      ("53f52acc1c371006e1138f00", "53f52acc1c371006e1138f05") -> 0.0750739,
      
      ("54b8748ea0530f2245e7a955", "54b8748ea0530f2245e7a955") -> 0,
      ("54b8748ea0530f2245e7a955", "54b8748ea0530f2245e7a94f") -> 0.00969955,        
      ("54b8748ea0530f2245e7a955", "53f52ac91c371006e1138e73") -> 0.128377,
      ("54b8748ea0530f2245e7a955", "53f52acc1c371006e1138f05") -> 0.104505,
      
      ("54b8748ea0530f2245e7a94f", "54b8748ea0530f2245e7a94f") -> 0,
      ("54b8748ea0530f2245e7a94f", "53f52ac91c371006e1138e73") -> 0.128377,
      ("54b8748ea0530f2245e7a94f", "53f52acc1c371006e1138f05") -> 0.104505,

      ("53f52ac91c371006e1138e73", "53f52ac91c371006e1138e73") -> 0,
      ("53f52ac91c371006e1138e73", "53f52acc1c371006e1138f05") -> 0.0573843,
      
      ("53f52acc1c371006e1138f05", "53f52acc1c371006e1138f05") -> 0
    )


  def main(args: Array[String]) {
    val fileName = getClass getResource "/comparator_test.json"
    val source = Source fromURL fileName

    val structs = source getLines() flatMap StructureParser.parse

    val distances = structs.toList combinations 2 map {
      case List(s1, s2) =>
        val dist = Comparator.distance(s1, s2).get
        val refDist = reflResults getOrElse ((s1.id, s2.id), reflResults((s2.id, s1.id)))
        ((s1.id, s2.id), (dist, Math.abs(refDist - dist)))
    }

    distances foreach {
      case (ids, (dist, err)) =>
        println(s"$ids -> distance=$dist, \terror=$err")
    }

  }
}
