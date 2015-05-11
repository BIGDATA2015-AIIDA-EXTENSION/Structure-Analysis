package ch.epfl

import ch.epfl.structure.{ Structure, StructureParser, StructureParserIvano }
import ch.epfl.comparison.Comparator
import org.apache.spark.{ SparkConf, SparkContext }
import org.apache.spark.rdd.PairRDDFunctions

object Main {

  def main(args: Array[String]) {

    val conf = new SparkConf() setAppName "Structure Analysis"
    val sc = new SparkContext(conf)
    val structuresDir = "/projects/aiida/"

    val syntheticStructures = {
      val fileName = structuresDir + "structures.json"
      sc.textFile(fileName)
        .flatMap(StructureParser.parse)
        .filter(_.nbElements == 1)
        .map(renameSpecies)
        .map(s => (s, Comparator.getCompData(s)))
    }

    val naturalStructures = {
      val fileName = structuresDir + "natural_structures.json"
      sc.textFile(fileName)
        .flatMap(StructureParserIvano.parse)
        .map(Structure.convertIvano)
        .filter(_.nbElements == 1)
        .map(renameSpecies)
        .map(s => (s, Comparator.getCompData(s)))
    } collect ()

    val similar = syntheticStructures map { case (synthetic, syntheticData) =>
      val matchingSimilar = naturalStructures collect {
        case (natural, naturalData) if (Comparator areComparable (syntheticData, naturalData))
                                    && (Comparator areSimilar (syntheticData, naturalData)) =>
          (natural.id, (Comparator distance (syntheticData, naturalData)))
      }
      (synthetic.id, matchingSimilar)
    }

    similar.saveAsTextFile("/projects/aiida/similar_structures")

  }

  private def renameSpecies(structure: Structure): Structure = {
    require(structure.nbElements == 1)
    val newSites = structure.struct.sites map { site =>
      val newSpecies = site.species map { specie =>
        specie.copy(element = "X")
      }
      site.copy(species = newSpecies)
    }

    structure.copy(struct = structure.struct.copy(sites = newSites))
  }

}
