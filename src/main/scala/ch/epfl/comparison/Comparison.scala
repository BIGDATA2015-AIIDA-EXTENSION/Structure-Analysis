package ch.epfl.comparison

import ch.epfl.structure.{ Structure, StructureParser, StructureParserIvano }
import org.apache.spark.{ SparkConf, SparkContext }
import org.apache.spark.rdd.PairRDDFunctions

object Comparison {

  def compareStructures(args: Array[String]): Unit = {
    val conf = new SparkConf() setAppName "Structure Analysis"
    val sc = new SparkContext(conf)
    val structuresDir = "/projects/aiida/"

    val syntheticStructures = {
      val fileName = structuresDir + "structures.json"
      sc.textFile(fileName)
        .flatMap(StructureParser.parse)
        .filter(_.nbElements == 1)
        .flatMap(renameSpecies)
        .map(_.scaled)
        .map(s => (s, Comparator getCompData s))
    }

    val naturalStructures = {
      val fileName = structuresDir + "natural_structures.json"
      sc.textFile(fileName)
        .flatMap(StructureParserIvano.parse)
        .map(Structure.convertIvano)
        .filter(_.nbElements == 1)
        .flatMap(renameSpecies)
        .map(_.scaled)
        .map(s => (s, Comparator getCompData s))
    } collect ()

    val similar = syntheticStructures map { case (synthetic, syntheticData) =>
      val matchingSimilar = naturalStructures collect {
        case (natural, naturalData) if (Comparator areComparable (syntheticData, naturalData))
                                    && (Comparator areSimilar (syntheticData, naturalData)) =>
          (natural.id, (Comparator distance (syntheticData, naturalData)))
      }
      (synthetic.id, matchingSimilar)
    }

    similar saveAsTextFile (structuresDir + "similar_structures")

  }

  private def renameSpecies(structure: Structure): List[Structure] = {

    def placeholderName(x: Int) = s"EL$x"

    structure.elements.distinct.permutations.toList map { elements =>
      val substitutions = elements.zipWithIndex.map { case (e, i) => (e, placeholderName(i)) }.toMap

      val newSites = structure.struct.sites map { site =>
        val newSpecies = site.species map { specie =>
          specie.copy(element = substitutions(specie.element))
        }
        site.copy(species = newSpecies)
      }

      structure.copy(struct = structure.struct.copy(sites = newSites))
    }
  }
}
