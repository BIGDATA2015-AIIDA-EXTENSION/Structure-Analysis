package ch.epfl.computations

import breeze.linalg.DenseMatrix
import breeze.plot._
import ch.epfl.structure.Param

import scala.util.parsing.combinator.syntactical.StandardTokenParsers

object GraphMaker {

  object MapParser extends StandardTokenParsers {

    lexical.delimiters ++= List("(", ",", ")", ".")
    lexical.reserved ++= List("MapID", "List", "Param")

    val doubleParser: Parser[Double] = numericLit ~ "." ~ numericLit ^^ {
      case a ~ "." ~ b => (a + "." + b).toDouble
    }

    val parseDDI: Parser[(Double, Double, Int)] = "(" ~> doubleParser ~ "," ~ doubleParser ~ "," ~ numericLit <~ ")" ^^ {
      case x ~ "," ~ y ~ "," ~ v => (x, y, v.toInt)
    }
    val parseList: Parser[List[(Double, Double, Int)]] = "List" ~ "(" ~> repsep(parseDDI, ",") <~ ")"

    val parseParam: Parser[Param] = "Param" ~ "(" ~> doubleParser ~ "," ~ doubleParser ~ "," ~ numericLit ~ "," ~ numericLit ~ "," ~ doubleParser <~ ")" ^^ {
      case a ~ "," ~ b ~ "," ~ c ~ "," ~ d ~ "," ~ e => Param(a, b, c.toInt, d.toInt, e)
    }

    val parseMapID: Parser[MapID] = "MapID" ~ "(" ~> doubleParser ~ "," ~ doubleParser ~ "," ~ ident <~ ")" ^^ {
      case a ~ "," ~ b ~ "," ~ c  => MapID(a, b, c)
    }

    val genParse: Parser[(MapID, List[(Double, Double, Int)])] = "(" ~> parseMapID ~ "," ~ parseList <~ ")" ^^ {
      case mapId ~ "," ~ list => (mapId, list)
    }

    def parseEverything(line: String): (MapID, List[(Double, Double, Int)]) = {
      val tokens = new lexical.Scanner(line)
      phrase(genParse)(tokens).get
    }

  }

  def generatePlots(args: Array[String]): Unit = {
    val source = scala.io.Source.fromFile(args(0))
    val lines = source.getLines()
    lines.map(MapParser.parseEverything) foreach {
      case (k, v) => generatePlot(v, ABEpsilon, BBEpsilon, k)
    }
    System.exit(0)
  }

  def generatePlot(mapPoints: List[(Double, Double, Int)], mapAxisX: MapAxis, mapAxisY: MapAxis, mapID: MapID): Unit = {
    val maxX = (mapPoints.maxBy(point => point._1)._1 * 10).toInt + 1
    val maxY = (mapPoints.maxBy(point => point._2)._2 * 10).toInt + 1
    val maxZ = mapPoints.maxBy(point => point._2)._3
    val m = DenseMatrix.zeros[Double](maxY, maxX)
    mapPoints.foreach {
      p => m.update((p._2 * 10).toInt, (p._1 * 10).toInt, p._3.toDouble / maxZ)
    }
    val f = Figure()
    f.subplot(0).xlabel = "" + mapAxisX
    f.subplot(0).ylabel = "" + mapAxisY
    f.visible = false
    f.subplot(0) += image(m)
    f.saveas("plot" + mapID + ".png")
    f.clear()
  }
}
