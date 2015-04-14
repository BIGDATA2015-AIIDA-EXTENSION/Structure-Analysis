package ch.epfl

import ch.epfl.structure.StructureParser
import org.apache.spark.{SparkConf, SparkContext}

import scala.io.Source

object Main {
  def main(args: Array[String]) {
    // val fileName = getClass getResource "/structures.json"
    // val source = Source fromURL fileName

    // val structs = source getLines() flatMap StructureParser.parse
    // structs foreach println

    val fileName = "/projects/aiida/structures.json"
    val conf = new SparkConf() setAppName "Structure Analysis"
    val sc = new SparkContext(conf)

    val lines = sc textFile fileName
    val structs = lines flatMap StructureParser.parse
    structs foreach println
  }
}
