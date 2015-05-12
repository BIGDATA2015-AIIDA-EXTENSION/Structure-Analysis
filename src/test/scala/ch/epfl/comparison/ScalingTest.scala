package ch.epfl.comparison

import ch.epfl.structure._

import org.scalatest.FunSuite

class ScalingTest extends FunSuite {

  test("A 5x5x5 structure should be scaled to a 1x1x1 structure") {

    val originalSpecies =
      Seq(
        Species(occu = 1, element = "A")
      )

    val originalSites =
      Seq(
        Site(
          abc = Seq(0.5, 0.5, 0.5),
          xyz = Seq(2.5, 2.5, 2.5),
          species = originalSpecies)
      )

    val originalLattice =
      Lattice(
        gamma = 0,
        a = 5,
        b = 5,
        c = 5,
        matrix = Seq(Seq(0, 0, 5), Seq(0, 5, 0), Seq(5, 0, 0)),
        volume = 5 * 5 * 5,
        alpha = 0,
        beta = 0)

    val originalStruct =
      Struct(
        sites = originalSites,
        lattice = originalLattice)

    val originalStructure =
      Structure(
        id = "dummy",
        elements = Seq("A"),
        energy = 0,
        pressure = 0,
        spaceGroup = SpaceGroup.empty,
        unitCellFormula = Map.empty,
        struct = originalStruct,
        reducedCellFormula = Map.empty,
        nbElements = 1,
        nbSites = 1,
        chemsys = "",
        potential = Potential.empty,
        prettyFormula = "",
        anonymousFormula = "",
        energyPerSite = 0)

    /******************/

    val expectedSites =
      Seq(
        Site(
          abc = Seq(0.5, 0.5, 0.5),
          xyz = Seq(0.5, 0.5, 0.5),
          species = originalSpecies)
      )

    val expectedLattice =
      Lattice(
        gamma = 0,
        a = 1,
        b = 1,
        c = 1,
        matrix = Seq(Seq(0, 0, 1), Seq(0, 1, 0), Seq(1, 0, 0)),
        volume = 1 * 1 * 1,
        alpha = 0,
        beta = 0)

    val expectedStruct =
      Struct(
        sites = expectedSites,
        lattice = expectedLattice)

    val expectedStructure =
      originalStructure.copy(struct = expectedStruct)

    val scaledStructure = originalStructure.scaled

    assert(expectedSites == scaledStructure.struct.sites)
    assert(expectedLattice == scaledStructure.struct.lattice)
    assert(expectedStruct == scaledStructure.struct)
    assert(scaledStructure == expectedStructure)

  }
}
