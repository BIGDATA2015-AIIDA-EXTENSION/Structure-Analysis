package ch.epfl.comparison

import ch.epfl.structure.Structure


object Comparison {

  val alphabet = Vector("A", "B")

  def renameSpecies(struct: Structure): List[Structure] = {
    require(struct.nbElements <= alphabet.length)
    val elems = alphabet take struct.nbElements
    val elemSet = elems.toSet

    struct.elements.toList.permutations.toList map { oldElems =>
      val substitutions = (oldElems.zipWithIndex map {
        case (e, i) => (e, alphabet(i))
      }).toMap

      val sites = struct.struct.sites map { site =>
        val newSpecies = site.species map { specie =>
          specie.copy(element = substitutions(specie.element))
        }
        site.copy(species = newSpecies)
      }

      val prettyFormula = (elems map { e =>
        val count = sites count (_.species.exists(_.element == e))
        if (count == 1) e else e + count
      }).mkString

      struct.copy(elements = elemSet,
        struct = struct.struct.copy(sites = sites),
        prettyFormula = prettyFormula)
    }
  }
}
