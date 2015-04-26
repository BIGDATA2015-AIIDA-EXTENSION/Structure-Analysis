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
object StructureParserIvano {

  def parse(line: String): Option[StructureIvano] = for {
    json <- Try(Json parse line).toOption
    result <- json.validate[StructureIvano].asOpt
  } yield result

  def parseDebug(line: String): Unit = {
    val json = Json.parse(line)
    val result = json.validate[StructureIvano]
    println(result)
  }

  private implicit val siteIvanoReads: Reads[SiteIvano] = (

    (JsPath \ "position"                ).read[Seq[Double]] and
      (JsPath \ "kind_name"                ).read[String]
    )(SiteIvano.apply _)

  private implicit val structureIvanoReads: Reads[StructureIvano] = (
    (JsPath \ "uuid"                ).read[String] and
      (JsPath \ "cell"                ).read[Seq[Seq[Double]]] and
      (JsPath \ "sites"                ).read[Seq[SiteIvano]] and
      (JsPath \ "pbc"                ).read[Seq[Boolean]]
  )(StructureIvano.apply _)

}
//(JsPath \ "uuid"                ).read[String] and