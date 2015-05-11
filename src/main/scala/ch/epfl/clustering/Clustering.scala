package ch.epfl.clustering
import scala.collection.mutable.{HashMap => MHashMap}

/**
 * Created by lukas on 07/05/15.
 */
object Clustering {

  def cluster[T](id: String, elems: List[T], distance: (T, T) => Double, nbCluster: Int): ClusteredStructure[T] = {
    cluster[T](id, elems, distance, nbCluster to nbCluster).head
  }


  def cluster[T](id: String, elems: List[T], distance: (T, T) => Double, nbCluster: Range): List[ClusteredStructure[T]] = {

    if(nbCluster.start < 1 || nbCluster.end > elems.size)
      sys.error(s"$nbCluster is not a valid cluster number for ${elems.size} elements.")

    type S = (T, Int)
    val elements = elems.zipWithIndex
    val elemInCluster = MHashMap[S, Int]()
    val clusters = MHashMap[Int, Clust]()
    val nbElems = elems.size
    val nbDist = nbElems*(nbElems-1)/2
    var distances = new Array[DistHolder](nbDist)
    var clusterings = List[ClusteredStructure[T]]()

    case class Clust(id: Int, var elems: List[S]) {

      def fusionCluster(cl: Clust): Unit = {
        this.elems = this.elems ++ cl.elems
      }

    }
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
      var nbIter = nbElems - nbCluster.start
      var index = 0

      while(nbIter > 0) {
        while(distances(index).sameCluster()) index +=1
        val cl1 = distances(index).getElem1Cluster
        val cl2 = distances(index).getElem2Cluster

        cl1.fusionCluster(cl2)

        cl2.elems.foreach(e => elemInCluster.put(e, cl1.id))
        clusters.remove(cl2.id)

        index +=1
        nbIter -=1

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


case class ClusteredStructure[T](id: String, clusters: List[Cluster[T]])

case class Cluster[T](elems: List[T])
