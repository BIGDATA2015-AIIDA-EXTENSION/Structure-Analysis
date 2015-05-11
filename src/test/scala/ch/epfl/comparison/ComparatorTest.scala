package ch.epfl.comparison

import ch.epfl.structure.StructureParser
import org.scalatest.FunSuite

import scala.io.Source

/**
 * Created by renucci on 07/05/15.
 */
class ComparatorTest extends FunSuite {
  val refResults: Map[(String, String), Double] =
    Map(
      ("54b87493a0530f2245e7a9a1", "54b87493a0530f2245e7a9a1") -> 0,
      ("54b87493a0530f2245e7a9a1", "54f582b401162b54f25762ae") -> 0.129087,
      ("54b87493a0530f2245e7a9a1", "54b87492a0530f2245e7a995") -> 0.309258,
      ("54b87493a0530f2245e7a9a1", "54b87491a0530f2245e7a982") -> 0.135633,
      ("54b87493a0530f2245e7a9a1", "53f52acc1c371006e1138f00") -> 0.136592,
      ("54b87493a0530f2245e7a9a1", "54b8748ea0530f2245e7a955") -> 0.187729,
      ("54b87493a0530f2245e7a9a1", "54b8748ea0530f2245e7a94f") -> 0.187729,
      ("54b87493a0530f2245e7a9a1", "53f52ac91c371006e1138e73") -> 0.124715,
      ("54b87493a0530f2245e7a9a1", "53f52acc1c371006e1138f05") -> 0.123937,

      ("54f582b401162b54f25762ae", "54f582b401162b54f25762ae") -> 0,
      ("54f582b401162b54f25762ae", "54b87492a0530f2245e7a995") -> 0.266159,
      ("54f582b401162b54f25762ae", "54b87491a0530f2245e7a982") -> 0.0823683,
      ("54f582b401162b54f25762ae", "53f52acc1c371006e1138f00") -> 0.105724,
      ("54f582b401162b54f25762ae", "54b8748ea0530f2245e7a955") -> 0.104044,
      ("54f582b401162b54f25762ae", "54b8748ea0530f2245e7a94f") -> 0.104044,
      ("54f582b401162b54f25762ae", "53f52ac91c371006e1138e73") -> 0.100014,
      ("54f582b401162b54f25762ae", "53f52acc1c371006e1138f05") -> 0.0926495,

      ("54b87492a0530f2245e7a995", "54b87492a0530f2245e7a995") -> 0,
      ("54b87492a0530f2245e7a995", "54b87491a0530f2245e7a982") -> 0.269649,
      ("54b87492a0530f2245e7a995", "53f52acc1c371006e1138f00") -> 0.280604,
      ("54b87492a0530f2245e7a995", "54b8748ea0530f2245e7a955") -> 0.270365,
      ("54b87492a0530f2245e7a995", "54b8748ea0530f2245e7a94f") -> 0.270365,
      ("54b87492a0530f2245e7a995", "53f52ac91c371006e1138e73") -> 0.279702,
      ("54b87492a0530f2245e7a995", "53f52acc1c371006e1138f05") -> 0.271745,

      ("54b87491a0530f2245e7a982", "54b87491a0530f2245e7a982") -> 0,
      ("54b87491a0530f2245e7a982", "53f52acc1c371006e1138f00") -> 0.0247718,
      ("54b87491a0530f2245e7a982", "54b8748ea0530f2245e7a955") -> 0.0555921,
      ("54b87491a0530f2245e7a982", "54b8748ea0530f2245e7a94f") -> 0.0551285,
      ("54b87491a0530f2245e7a982", "53f52ac91c371006e1138e73") -> 0.0507691,
      ("54b87491a0530f2245e7a982", "53f52acc1c371006e1138f05") -> 0.0610846,

      ("53f52acc1c371006e1138f00", "53f52acc1c371006e1138f00") -> 0,
      ("53f52acc1c371006e1138f00", "54b8748ea0530f2245e7a955") -> 0.116987,
      ("53f52acc1c371006e1138f00", "54b8748ea0530f2245e7a94f") -> 0.116987,
      ("53f52acc1c371006e1138f00", "53f52ac91c371006e1138e73") -> 0.0521669,
      ("53f52acc1c371006e1138f00", "53f52acc1c371006e1138f05") -> 0.0626708,

      ("54b8748ea0530f2245e7a955", "54b8748ea0530f2245e7a955") -> 0,
      ("54b8748ea0530f2245e7a955", "54b8748ea0530f2245e7a94f") -> 0.00969955,
      ("54b8748ea0530f2245e7a955", "53f52ac91c371006e1138e73") -> 0.128377,
      ("54b8748ea0530f2245e7a955", "53f52acc1c371006e1138f05") -> 0.104505,

      ("54b8748ea0530f2245e7a94f", "54b8748ea0530f2245e7a94f") -> 0,
      ("54b8748ea0530f2245e7a94f", "53f52ac91c371006e1138e73") -> 0.128377,
      ("54b8748ea0530f2245e7a94f", "53f52acc1c371006e1138f05") -> 0.104505,

      ("53f52ac91c371006e1138e73", "53f52ac91c371006e1138e73") -> 0,
      ("53f52ac91c371006e1138e73", "53f52acc1c371006e1138f05") -> 0.0573843,

      ("53f52acc1c371006e1138f05", "53f52acc1c371006e1138f05") -> 0
    )

  test("Comparator's results should match Martin's results") {
    val fileName = getClass getResource "/comparator_test.json"
    val source = Source fromURL fileName

    val structs = source getLines() flatMap StructureParser.parse
    val distances = structs.toList combinations 2 map {
      case List(s1, s2) =>
        val dist = Comparator.distance(s1, s2)
        val refDist = refResults getOrElse((s1.id, s2.id), refResults((s2.id, s1.id)))
        ((s1.id, s2.id), (dist, Math.abs(refDist - dist)))
    }

    distances foreach {
      case (ids, (dist, err)) =>
        println(s"$ids -> distance=$dist, \terror=$err")
    }
  }
}
