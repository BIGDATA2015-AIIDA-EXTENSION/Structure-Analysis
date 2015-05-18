package ch.epfl.structure

import breeze.linalg.{DenseMatrix, DenseVector, inv}
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.util.Try

/**
 * The field naming in Structure must match the json file
 * in order to use JSON Macro Inception.
 * Otherwise you can't use the 1 line version and should use
 * JSON Reads Combinators
 */
object NaturalStructureParser {

  def parse(line: String): Option[Structure] = for {
    json   <- Try(Json parse line).toOption
    result <- json.validate[NStructure].asOpt
  } yield convertStructure(result)

  private case class NStructure(uuid: String,
                                cell: Seq[Seq[Double]],
                                cellAngles: Seq[Double],
                                cellLengths: Seq[Double],
                                cellVolume: Double,
                                sites: Seq[NSite],
                                pbc: Seq[Boolean])

  private case class NSite(position: Seq[Double], kindName: String, properties: NProperties)

  private case class NProperties(weights: Seq[Double], mass: Double)

  private implicit val propertiesIvanoReads = Json.reads[NProperties]

  private implicit val siteIvanoReads: Reads[NSite] = (
    (JsPath \ "position"  ).read[Seq[Double]] and
    (JsPath \ "kind_name" ).read[String] and
    (JsPath \ "properties").read[NProperties]
  )(NSite.apply _)

  private implicit val structureIvanoReads: Reads[NStructure] = (
    (JsPath \ "uuid"        ).read[String] and
    (JsPath \ "cell"        ).read[Seq[Seq[Double]]] and
    (JsPath \ "cell_angles" ).read[Seq[Double]] and
    (JsPath \ "cell_lengths").read[Seq[Double]] and
    (JsPath \ "cell_volume" ).read[Double] and
    (JsPath \ "sites"       ).read[Seq[NSite]] and
    (JsPath \ "pbc"         ).read[Seq[Boolean]]
  )(NStructure.apply _)

  private def convertStructure(nstruct: NStructure): Structure = {
    val struct = convertStruct(nstruct)
    val id                 = nstruct.uuid
    val elements           = (nstruct.sites map (_.kindName)).toSet
    val energy             = 0    // Not known
    val pressure           = 0    // Not known
    val spaceGroup         = null // Not known
    val unitCellFormula    = null // Not known
    val reducedCellFormula = null // Not known
    val nbElements         = elements.size
    val nbSites            = struct.sites.size
    val chemsys            = null // Not known
    val potential          = null // Not known
    val prettyFormula      = null // Not known
    val anonymousFormula   = null // Not known
    val energyPerSite      = 0    // Not known

    Structure(id, elements, energy, pressure, spaceGroup,
      unitCellFormula, struct, reducedCellFormula, nbElements,
      nbSites, chemsys, potential, prettyFormula, anonymousFormula,
      energyPerSite)
  }

  private def convertStruct(struct: NStructure): Struct = {
    val l = DenseMatrix.tabulate(3, 3) { case (i, j) =>
      struct.cell(i)(j)
    }

    val lInverse = inv(l)

    val sites = struct.sites map {
      case NSite(position, kindName, _) =>
        val c = DenseVector(position.toArray)
        val abc = lInverse * c
        Site(abc.toArray.toList, position, List(Species(1, kindName)))
    }

    val gamma = 0.0 // Not known
    val a = struct.cellLengths(0)
    val b = struct.cellLengths(1)
    val c = struct.cellLengths(2)
    val matrix = struct.cell
    val volume = struct.cellVolume
    val alpha = 0.0 // Not known
    val beta = 0.0  // Not known

    val lattice = Lattice(gamma, a, b, c, matrix, volume, alpha, beta)

    Struct(sites, lattice)
  }




}