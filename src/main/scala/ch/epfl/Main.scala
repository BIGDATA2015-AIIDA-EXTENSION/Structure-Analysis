package ch.epfl

import ch.epfl.structure.{StructureIvano, Structure, StructureParserIvano, StructureParser}


import ch.epfl.clustering.{ClusterStruct, Clustering}
import ch.epfl.computations.AiidaComputations._
import ch.epfl.computations.GraphMaker._

import play.api.libs.json._

import sext._

import scala.io.Source

object Main {
  def main(args: Array[String]) {

    args(2) match {
      case "c" => compute(args)
      case "p" => generatePlots(args)
      case "cl" => ClusterStruct.compute(args)
      case _ => println("Missing argument.")
    }


// ------ Get all records from a file

//    val fileName2 = getClass getResource "/structures_ivano_new_no_NaN.json"
//    val source2 = Source fromURL fileName2
//
//    val structs2 = source2 getLines() flatMap { x: String =>
//          StructureParserIvano.parse(x)
//        }
//
//    for ( s <- structs2) {
//      println(s)
//      println(Structure.convertIvano(s));
//    }

    //---- Get a single record from a file
//
/*

    val fileName3 = getClass getResource "/structures_ivano_new_no_NaN.json"
    val source3 = Source fromURL fileName3
    val testjson3 = source3.getLines().next()
    val testobject3 = StructureParserIvano.parse(testjson3)
    println(testjson3)
    for (s <- testobject3) {
      println(s)
      println(Structure.convertIvano(s).valueTreeString)
    }
*/
  }
}
