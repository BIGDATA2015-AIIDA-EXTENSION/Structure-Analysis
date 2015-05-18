package ch.epfl.mapgeneration

import ch.epfl.structure._
import org.apache.hadoop.io.NullWritable
import org.apache.hadoop.mapred.lib.MultipleTextOutputFormat
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

    // Transforms the result into a better readable format and outputs it to the output location using the formatter
    // defined below
    groups.map(prettyOutput).saveAsHadoopFile(args(1), classOf[MapID], classOf[String],
      classOf[RDDMultipleTextOutputFormat])

    // Terminates spark context
    sc.stop()
  }

  /**
   * Transforms a map into a readable format
   * @param map actual map
   * @tparam T The type of the property kept in the map
   * @return A multiline String representing the map
   */
  def prettyOutput[T] (map: (MapID, List[(Double, Double, T)])): (MapID,String) = {
    // all the properties in the map in csv format : x, y, property
    val properties = map._2.map(v => s"${v._1}, ${v._2}, ${v._3}").mkString("\n")
    (map._1, properties)
  }


}

/**
 * Formatting class to output each map in a different file
 * The resulting maps are in a key-value format where the key a `MapID` and the map is transformed to a csv format multiline
 * string.
 * The use of type Any allows to perform some tricks that avoids spark outputting the MapID on each line of the file
 */
class RDDMultipleTextOutputFormat extends MultipleTextOutputFormat[Any, Any] {
  /**
   * Returns a `NullWritable` so that the key is not written on each line of the file.
   * @param key the `MapID`
   * @param value the value associated with the `MapID`
   * @return a `NullWritable`
   */
  override def generateActualKey(key: Any, value: Any): Any =
    NullWritable.get()

  /**
   * Generates a file name given a key-value pair
   * @param key will be a `MapID`
   * @param value a value
   * @param name some string (not used here)
   * @return THe name of the file in the format `anonymousFormula`-`fixedParameter1`-`fixedParameter2`
   */
  override def generateFileNameForKeyValue(key: Any, value: Any, name: String): String = {
    val actKey = key.asInstanceOf[MapID]
    s"${actKey.anonymousFormula}-${actKey.val1}-${actKey.val2}"
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
 * @param fixedParams fixed parameters in the maps
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
      s: Structure => functions.foldLeft(true) { case (acc, fun) => acc && fun(s) }
    }
    // This is a list of the boolean functions we need to filter
    val booleanFunctions = List(combinedCondition(fixedValues), isInRange(rangeX, mapAxisX), isInRange(rangeY, mapAxisY))

    // Actual filtering of the RDD
    rdd.filter(conjunction(booleanFunctions))
  }


  /**
   * Generate the maps from a `RDD[Structure]` object. It does so by grouping all the comparable structures, dividing
   * them into similar parameters structures, extracting the ones with lowest energy per site and putting them into maps
   * @param rdd the structures to generate the maps
   * @return an RDD containing the maps in the following format : map identifier, list of (x, y, property)
   */
  def getMap(rdd: RDD[Structure]): RDD[(MapID, List[(Double, Double, T)])] = {

    /**
     * Generates a tuple containing only the fixed parameters values, the anonymous formula, the varying parameters and
     * the AA parameter (which is never used as axis), this tuple will be used to group structures with other that have
     * the same parameters
     * @param structure the `Structure` that needs to be transformed
     * @return the tuple
     */
    def paramsKey(structure: Structure) = {
      val p = structure.potential.params
      (getAxisValue(fixedParams._1, p), getAxisValue(fixedParams._2, p), structure.anonymousFormula,
        getAxisValue(mapAxisX, p), getAxisValue(mapAxisY, p), structure.potential.params.aa)
    }

    /**
     * Transforms a `Structure` into a `MapID`
     * @param struct the `Structure` that needs to be transformed
     * @return a `MapID` corresponding to the `Structure`
     */
    def structToMapID(struct: Structure): MapID =
      MapID(getAxisValue(fixedParams._1, struct.potential.params), getAxisValue(fixedParams._2, struct.potential.params), struct.prettyFormula)

    /**
     * Transforms a `Structure` into a `MapElement`
     * @param struct the `Structure` that needs to be transformed
     * @return a `MapElement` corresponding to the `Structure`
     */
    def structToMapElement(struct: Structure): MapElement[T] =
      MapElement[T](structToMapID(struct), getAxisValue(mapAxisX, struct.potential.params), getAxisValue(mapAxisY, struct.potential.params), extractProperty(struct))

    // filters the structures
    val usefulStructures = getFilteredResults(rdd)

    // for each structure generate a pair (paramsKey, structure)
    val structuresMappedToKeyValue = usefulStructures.map(s => (paramsKey(s), s))

    // reduceByKey generates an Iterable (not an RDD !) by grouping together the structures with the same key and
    // then reducing each group by applying an operation to each pair of structures in the groups, this cooperation should
    // return another structure, here we return the one with the smallest energyPerSite so that in the end we get only
    // the one with the minimum energyPerSite for each group. Note that this reduction operation costs a lot, because
    // it involves shuffling across the cluster but, the way spark is built, it will reduce on each node and then only send
    // the minimums. of each node to be compared to the rest, this means that the communication load will be quite low.
    // This kind of operation should be done with precaution, here it is okay because the result of the reduction is small
    // compared to the initial data.
    val structuresWithMinEnergy = structuresMappedToKeyValue.reduceByKey { case (s1, s2) => if (s1.energyPerSite < s2.energyPerSite) s1 else s2 }

    // This final transformation transforms the pair paramsKey, structure to a MapID, MapElement pair and then groups
    // all said pair by MapID which results into an iterable that contains pairs made of MapID, list of (MapID, MapElement).
    // This iterable is then transformed into the expected result which is a List of MapID, list of (x, y, property) pairs
    structuresWithMinEnergy.map { case (param_id, struct) => (structToMapID(struct), structToMapElement(struct)) }
      .groupBy(_._1).map { case (id, iterable) => (id, iterable.map { case (k, v) => (v.x, v.y, v.value) }.toList) }

  }
}