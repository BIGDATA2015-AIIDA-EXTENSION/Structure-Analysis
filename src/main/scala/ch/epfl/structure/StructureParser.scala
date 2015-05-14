package ch.epfl.structure

import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.util.Try

/**
 * The field naming in Structure must match the json file
 * in order to use JSON Macro Inception.
 * Otherwise you can't use the 1 line version and should use
 * JSON Reads Combinators
 */
object StructureParser {

  def parse(line: String): Option[Structure] = for {
    json   <- Try(Json parse line).toOption
    result <- json.validate[Structure].asOpt
  } yield result

  private implicit val paramReads = Json.reads[Param]

  private implicit val paramsReads: Reads[Params] = (
    (JsPath \ "A~A").read[Param] and
    (JsPath \ "B~B").read[Param] and
    (JsPath \ "A~B").read[Param]
  )(Params.apply _)

  private implicit val speciesReads = Json.reads[Species]

  private implicit val potentialReads: Reads[Potential] = (
    (JsPath \ "name") .read[String] and
    (JsPath \ "params").read[Params]
  )(Potential.apply _)

  private implicit val siteReads = Json.reads[Site]

  private implicit val latticeReads = Json.reads[Lattice]

  private implicit val structReads = Json.reads[Struct]


  private implicit val spaceGroupReads: Reads[SpaceGroup] = (
    (JsPath \ "point_group"   ).read[String] and
    (JsPath \ "source"        ).read[String] and
    (JsPath \ "crystal_system").read[String] and
    (JsPath \ "hall"          ).read[String] and
    (JsPath \ "symbol"        ).read[String] and
    (JsPath \ "number"        ).read[Int]
  )(SpaceGroup.apply _)

  private implicit val structureReads: Reads[Structure] = (
    (JsPath \ "_id" \ "$oid"        ).read[String] and
    (JsPath \ "elements"            ).read[Set[String]] and
    (JsPath \ "energy"              ).read[Double] and
    (JsPath \ "pressure"            ).read[Double] and
    (JsPath \ "spacegroup"          ).read[SpaceGroup] and
    (JsPath \ "unit_cell_formula"   ).read[Map[String, Int]] and
    (JsPath \ "structure"           ).read[Struct] and
    (JsPath \ "reduced_cell_formula").read[Map[String, Int]] and
    (JsPath \ "nelements"           ).read[Int] and
    (JsPath \ "nsites"              ).read[Int] and
    (JsPath \ "chemsys"             ).read[String] and
    (JsPath \ "potential"           ).read[Potential] and
    (JsPath \ "pretty_formula"      ).read[String] and
    (JsPath \ "anonymous_formula"   ).read[String] and
    (JsPath \ "energy_per_site"     ).read[Double]
  )(Structure.apply _)
}
