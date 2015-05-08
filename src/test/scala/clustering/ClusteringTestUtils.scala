package clustering

import ch.epfl.clustering.{Cluster, ClusteredStructure}

/**
 * Created by lukas on 08/05/15.
 */
object ClusteringTestUtils {
  def compareClusterings[T](cl1: ClusteredStructure[T], cl2: ClusteredStructure[T]): Boolean = {

    def removeFirst[S](list: List[S])(pred: (S) => Boolean): List[S] = {
      val (before, atAndAfter) = list.span(x => !pred(x))
      before ::: atAndAfter.drop(1)
    }

    def compareCluster(cluster1: Cluster[T], cluster2: Cluster[T]): Boolean = {
      def helper(elems1: List[T], elems2: List[T]): Boolean = {
        if (elems1.size == elems2.size) {
          (elems1, elems2) match {
            case (e1 :: Nil, e2 :: Nil) => e1 == e2
            case (e1 :: els1, els2) => helper(els1, removeFirst(els2)(e => e1 == e))
          }
        } else {
          false
        }
      }
      helper(cluster1.elems, cluster2.elems)
    }
    def helper(clusters1: List[Cluster[T]], clusters2: List[Cluster[T]]): Boolean = {

      if (clusters1.size == clusters2.size) {
        (clusters1, clusters2) match {
          case (c1 :: Nil, c2 :: Nil) => compareCluster(c1, c2)
          case (c1 :: cs1, cs2) =>
            def comp(c: Cluster[T]) = compareCluster(c1, c)
            helper(cs1, removeFirst(cs2)(comp))
        }
      } else {
        false
      }
    }
    helper(cl1.clusters, cl2.clusters)
  }

}
