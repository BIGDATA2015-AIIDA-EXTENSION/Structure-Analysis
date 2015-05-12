package ch.epfl.comparison

import ch.epfl.structure.{ Structure, StructureParser, NaturalStructureParser }
import org.apache.spark.{ SparkConf, SparkContext }
import org.apache.spark.rdd.PairRDDFunctions

object Comparison {

  def run(args: Array[String]): Unit = {
    if (args.length < 4) {
      println("Usage: spark-submit [spark options] <this jar>.jar <synthetic file> <natural file> comparison <output file>")
    } else {
      compareStructures(args)
    }
  }

  def compareStructures(args: Array[String]): Unit = {

    val inputSynthetic = args(0)
    val inputNatural   = args(1)
    val outputFile     = args(3)

    val conf = new SparkConf() setAppName "Structure Analysis"
    val sc = new SparkContext(conf)
    val structuresDir = "/projects/aiida/"

    val syntheticStructures = {
      val fileName = structuresDir + inputSynthetic
      sc.textFile(fileName)
        .flatMap(StructureParser.parse)
        .flatMap(renameSpecies)
        .map(_.scaled)
        .map(s => (s, Comparator getCompData s))
    }

    val naturalStructures = {
      val fileName = structuresDir + inputNatural
      sc.textFile(fileName)
        .flatMap(NaturalStructureParser.parse)
        .flatMap(renameSpecies)
        .map(_.scaled)
        .map(s => (s, Comparator getCompData s))
    } collect ()

    val similar = syntheticStructures map { case (synthetic, syntheticData) =>
      val nbElements = synthetic.nbElements
      val matchingSimilar = naturalStructures collect {
        case (natural, naturalData) if natural.nbElements == nbElements
                                    && (Comparator areComparable (syntheticData, naturalData))
                                    && (Comparator areSimilar (syntheticData, naturalData)) =>
          (natural.id, Comparator distance (syntheticData, naturalData))
      }
      (synthetic.id, matchingSimilar)
    }

    similar saveAsTextFile (structuresDir + outputFile)

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
