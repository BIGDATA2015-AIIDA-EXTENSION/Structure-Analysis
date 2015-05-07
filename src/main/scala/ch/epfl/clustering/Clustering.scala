package ch.epfl.clustering

/**
 * Created by lukas on 07/05/15.
 */
object Clustering {
  def cluster[T](elems: List[T], distance: (T, T) => Double): ClusteredStructure[T] = {
    ???
  }
}


case class ClusteredStructure[T](clusters: List[Cluster[T]])

case class Cluster[T](elems: List[T])
