package ch.epfl.heatmaps

import ch.epfl.structure._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

/**
 * Object containing the logic for a spark job generating maps.
 * This could be a main object.
 */
object MapGeneration {

  /**
   * Spark job example for generating maps.
   * @param args Same arguments as any main method. ''args(0)'' should be the path to input file and ''args(1)'' should
   *             be the output location, be assured that this output location does not already exist otherwise the job
   *             will fail
   */
  def compute(args: Array[String]) {
    // Standard spark context creation
    val sc = new SparkContext(new SparkConf().setAppName("MapGeneration"))

    // Taking each line of the input file into an RDD[String]
    val jsonStructures = sc.textFile("hdfs://" + args(0))

    // Applying parser to the read lines, those should be one structure per line
    val parsed = jsonStructures flatMap StructureParser.parse

    // Request for the parsed result to be cached
    parsed.cache()

    // Fixed parameters in each map
    val fixedParams = (ABSigma, BBSigma)
    // Values for the fixed parameters
    val fixedValues = List((0.4, 1.0), (0.6, 1.0), (0.8, 1.0), (1.0, 1.0), (1.2, 1.0), (1.4, 1.0), (1.6, 1.0), (1.1, 1.0))
    // Which parameter on X axis
    val axisX = ABEpsilon
    // Which parameter on Y axis
    val axisY = BBEpsilon
    // The type parameter is Int as the property we are keeping is the spaceGroup number
    // The extractor for the property is a simple anonymous function that gets the spaceGroup number, making use of the
    // underscore wildcard, it is equivalent to the following notation :
    // structure: Structure => structure.spaceGroup.number
    val mapViewer = new MapGenerator[Int](fixedParams, fixedValues, axisX, axisY)(_.spaceGroup.number)

    // Generation of the maps, one map per anonymousFormula and per pair of fixed values will be generated
    val groups = mapViewer.getMap(parsed)

    // Transforms the result into a better readable format and outputs it to the output location
    groups.map(prettyOutput(fixedParams, axisX, axisY)).saveAsTextFile("hdfs://" + args(1))

    // Terminates spark context
    sc.stop()
  }

  /**
   * Transforms a map into a readable format
   * @param fixed fixed parameters for this map
   * @param axisX parameter used as X axis
   * @param axisY parameter used as Y axis
   * @param map actual map
   * @tparam T The type of the property kept in the map
   * @return A multiline String representing the map
   */
  def prettyOutput[T](fixed : (MapAxis, MapAxis), axisX : MapAxis, axisY: MapAxis)
                     (map: (MapID, List[(Double, Double, T)])): String = {

    // Header that states which are the fixed parameters and their values and the anonymous formula
    val header = s"${fixed._1}: ${map._1.val1}\n" +
      s"${fixed._2}: ${map._1.val2}\n" +
      s"Formula : ${map._1.anonymousFormula}\n"
    // all the properties in the map in csv format : x, y, property
    val properties = map._2.map(v => s"${v._1}, ${v._2}, ${v._3}").mkString("\n")
    header + properties + "---------\n"
  }


}

/**
 * Trait that represents an abstract map axis
 */
trait MapAxis

/**
 * The following case objects are all the possible parameters that can be used as map axis
 */
case object AASigma extends MapAxis

case object AAEpsilon extends MapAxis

case object ABSigma extends MapAxis

case object ABEpsilon extends MapAxis

case object BBSigma extends MapAxis

case object BBEpsilon extends MapAxis

/**
 * Represents a map identifier, it uniquely defines a map in a given `MapGenerator`
 * @param val1 value of first fixed parameter
 * @param val2 value of the second fixed parameter
 * @param anonymousFormula anonymous formula of the element represented in the map
 */
case class MapID(val1: Double, val2: Double, anonymousFormula: String)

/**
 * Represents an element of a map
 * @param id the map identifier
 * @param x x position of the map element
 * @param y y position of the map element
 * @param value value of the property selected for this map
 * @tparam T the type of the property selected for this map
 */
case class MapElement[T](id: MapID, x: Double, y: Double, value: T)


/**
 * Class containing all the logic to generate maps for a given set of parameters and a selected property.
 * @param fixedParams ixed parameters in the maps
 * @param fixedValues values of the fixed parameters, every element in this list will lead to the generation of a
 *                    whole set of maps
 * @param mapAxisX parameter used as X axis
 * @param mapAxisY parameter used as Y axis
 * @param rangeX optional couple that states the range in which the X values should be, None by default (which means
 *               that all values will be kept)
 * @param rangeY optional couple that states the range in which the Y values should be, None by default (which means
 *               that all values will be kept)
 * @param extractProperty function that given a `Structure` returns the property we are interested in, usually a simple
 *                        function that returns one of the attributes of a `Structure`
 * @tparam T the type of the interesting property
 */
case class MapGenerator[T](fixedParams: (MapAxis, MapAxis), fixedValues: List[(Double, Double)],
                        mapAxisX: MapAxis, mapAxisY: MapAxis,
                        rangeX: Option[(Double, Double)] = None, rangeY: Option[(Double, Double)] = None)
                          (extractProperty: Structure => T) {

  /**
   * Extracts the value of any given `MapAxis` from a `Params` object.
   * @param mapAxis axis we are interested in knowing the value
   * @param p `Params` object from which we want to extract the value
   * @return the value of axis mapAxis in the `Params` object params
   */
  private def getAxisValue(mapAxis: MapAxis, p: Params): Double = mapAxis match {
    case AASigma => p.aa.sigma
    case AAEpsilon => p.aa.epsilon
    case BBSigma => p.bb.sigma
    case BBEpsilon => p.bb.epsilon
    case ABSigma => p.ab.sigma
    case ABEpsilon => p.ab.epsilon
  }


  /**
   * Given a value, an axis and a `Params` tests if the value of the axis in the `Params` object is the same as the
   * given value
   * @param mapAxis the axis to consider
   * @param value the given value
   * @param p the `Params` from which to get the value
   * @return the result of the comparison
   */
  private def condition(mapAxis: MapAxis, value: Double, p: Params): Boolean = {
    getAxisValue(mapAxis, p) == value
  }

  /**
   * Filters out the results that do not correspond to the specified fixed values and the given ranges.
   * @param rdd the RDD to filter
   * @return the filtered RDD
   */
  private def getFilteredResults(rdd: RDD[Structure]): RDD[Structure] = {

    /**
     * Combines the specified fixed values into a boolean function that compares the values in a `Structure` object to
     * the one specified.
     * @param fixedValues the set of specified fixed values
     * @return the comparison function
     */
    def combinedCondition(fixedValues: List[(Double, Double)]): Structure => Boolean = fixedValues match {
      case Nil => sys.error("error")
      case (c1, c2) :: Nil => s: Structure => condition(fixedParams._1, c1, s.potential.params) && condition(fixedParams._2, c2, s.potential.params)
      case (c1, c2) :: tail => s: Structure => (condition(fixedParams._1, c1, s.potential.params) && condition(fixedParams._2, c2, s.potential.params)) | combinedCondition(tail)(s)

    }

    /**
     * Creates a function that tells if a `Structure` object has given axis inside given range
     * @param rangeOpt the optional range
     * @param mapAxis the axis to check
     * @return as function that returns true if the rangeOpt is None or the mapAxis in the `Structure` is in the range,
     *         false otherwise
     */
    def isInRange(rangeOpt: Option[(Double, Double)], mapAxis: MapAxis): Structure => Boolean = rangeOpt match {
      case Some(bounds) =>
        s: Structure => bounds._1 <= getAxisValue(mapAxis, s.potential.params) && getAxisValue(mapAxis, s.potential.params) <= bounds._2
      case None =>
        s: Structure => true
    }

    /**
     * Creates a boolean conjunction of a set of boolean functions
     * @param functions the list of boolean functions
     * @return a function that is the conjunction of all the functions in the list
     */

    def conjunction(functions: List[Structure => Boolean]): Structure => Boolean = {
      s: Structure => functions.foldLeft(true){ case (acc, fun) => acc && fun(s)}
    }

    val booleanFunctions = List(combinedCondition(fixedValues), isInRange(rangeX, mapAxisX), isInRange(rangeY, mapAxisY))

    rdd.filter(conjunction(booleanFunctions))
  }


  def getMap(rdd: RDD[Structure]): RDD[(MapID, List[(Double, Double, T)])] = {

    def groupCond(structure: Structure) = {
      val p = structure.potential.params
      (getAxisValue(fixedParams._1, p), getAxisValue(fixedParams._2, p), structure.anonymousFormula, getAxisValue(mapAxisX, p), getAxisValue(mapAxisY, p), structure.potential.params.aa)
    }

    def structToMapID(struct: Structure): MapID =
      MapID(getAxisValue(fixedParams._1, struct.potential.params), getAxisValue(fixedParams._2, struct.potential.params), struct.prettyFormula)

    def structToMapElement(struct: Structure): MapElement[T] =
      MapElement[T](structToMapID(struct), getAxisValue(mapAxisX, struct.potential.params), getAxisValue(mapAxisY, struct.potential.params), extractProperty(struct))

    val usefulStructures = getFilteredResults(rdd)
    val structuresMappedToKeyValue = usefulStructures.map(s => (groupCond(s), s))

    val structuresWithMinEnergy = structuresMappedToKeyValue.reduceByKey { case (s1, s2) => if (s1.energyPerSite < s2.energyPerSite) s1 else s2 }

    structuresWithMinEnergy.map { case (param_id, struct) => (structToMapID(struct), structToMapElement(struct)) }
      .groupBy(_._1).map { case (id, iter) => (id, iter.map { case (k, v) => (v.x, v.y, v.value) }.toList) }

  }
}