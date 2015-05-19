package ch.epfl.clustering
import scala.collection.mutable.{HashMap => MHashMap}

/**
 * Created by lukas on 07/05/15.
 *
 */
object Clustering {

  /**
   *
   * This function clusters a list of elements of type T.
   *
   * @param id The id of the clustering
   * @param elems The list of elements to cluster
   * @param distance The distance function used to cluster the elements
   * @param nbCluster The expected number of clusters as Int
   * @tparam T The type of the elements to cluster
   * @return A ClusteredStructure that represents the clustering
   */
  def cluster[T](id: String, elems: List[T], distance: (T, T) => Double, nbCluster: Int): ClusteredStructure[T] = {
    cluster[T](id, elems, distance, nbCluster to nbCluster).head
  }

  /**
   *
   * This function clusters a list of elements of type T.
   *
   * @param id The id of the clustering
   * @param elems The list of elements to cluster
   * @param distance The distance function used to cluster the elements
   * @param nbCluster The expected number of clusters as a Range
   * @tparam T The type of the elements to cluster
   * @return A list of ClusteredStructure that represents the clustering for each number of cluster given in the range.
   */
  def cluster[T](id: String, elems: List[T], distance: (T, T) => Double, nbCluster: Range): List[ClusteredStructure[T]] = {

    if(nbCluster.start < 1 || nbCluster.end > elems.size)
      sys.error(s"$nbCluster is not a valid cluster number for ${elems.size} elements.")

    // We zip every element with it's id to make it unique
    type S = (T, Int)
    val elements = elems.zipWithIndex

    // This map keeps a mapping from an element to it's cluster's id
    val elemInCluster = MHashMap[S, Int]()
    // This map has a mapping from cluster id to cluster
    val clusters = MHashMap[Int, Clust]()
    val nbElems = elems.size
    val nbDist = nbElems*(nbElems-1)/2

    //This is an Array of all pairwise distances
    var distances = new Array[DistHolder](nbDist)

    //This is the list of results
    var clusterings = List[ClusteredStructure[T]]()

    // Case class that represents a Cluster and has a method to merge itself with another cluster
    case class Clust(id: Int, var elems: List[S]) {

      def fusionCluster(cl: Clust): Unit = {
        this.elems = this.elems ++ cl.elems
      }

    }
    // Case class that holds the distance between two elements
    case class DistHolder(e1: S, e2: S, distance: Double) {
      def sameCluster(): Boolean = {
        val e1Id = elemInCluster.get(e1).get
        val e2Id = elemInCluster.get(e2).get
        e1Id == e2Id
      }

      def getElem1Cluster: Clust = {
        clusters.get(elemInCluster.get(e1).get).get
      }

      def getElem2Cluster: Clust = {
        clusters.get(elemInCluster.get(e2).get).get
      }
    }

    def dataToClusteredStruct(): ClusteredStructure[T] = {
      ClusteredStructure(id, clusters.values.toList.map(cl => Cluster(cl.elems.map(_._1))))
    }

    def clusterize(): List[Clust] = {
      // We compute the number of merges we need to do
      var nbIter = nbElems - nbCluster.start
      var index = 0

      while(nbIter > 0) {
        //We iterate through the distances but ignore them if the elements are already in the same cluster
        while(distances(index).sameCluster()) index +=1
        val cl1 = distances(index).getElem1Cluster
        val cl2 = distances(index).getElem2Cluster

        // We fusion the two clusters
        cl1.fusionCluster(cl2)

        cl2.elems.foreach(e => elemInCluster.put(e, cl1.id))
        clusters.remove(cl2.id)

        index +=1
        nbIter -=1

        //If this number of cluster is in the range we want as output we add it to the result list
        if(nbCluster.contains(clusters.size)) {
          clusterings = dataToClusteredStruct() :: clusterings
        }
      }
      clusters.values.toList
    }

    var index = 0
    for (i <- 0 until nbElems) {
      elemInCluster.put(elements(i), i)
      clusters.put(i, Clust(i, elements(i) :: Nil))
      for (j <- i+1 until nbElems) {
        val e1 = elements(i)
        val e2 = elements(j)
        distances(index) = DistHolder(e1, e2, distance(e1._1, e2._1))
        index +=1
      }
    }

    distances = distances.sortBy(_.distance)
    clusterize()
    clusterings
  }
}

/**
 * Case class that represents a clustering. It has an id and a list of clusters.
 *
 * @param id id of the clustering
 * @param clusters list of clusters
 * @tparam T is the type of the elements that are clustered.
 */
case class ClusteredStructure[T](id: String, clusters: List[Cluster[T]])


/**
 * Case class that represents a cluster. A cluster has a list of elements that it contains. These elements are of type T
 *
 * @param elems list of elements in this cluster
 * @tparam T type of the elements of the cluster
 */
case class Cluster[T](elems: List[T])
