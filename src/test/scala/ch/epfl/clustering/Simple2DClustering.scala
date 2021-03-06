package ch.epfl.clustering

import org.scalatest.FunSuite

/**
 * Created by lukas on 08/05/15.
 */
class Simple2DClustering extends FunSuite {

  test("Clustering correctness 1") {
    val elems = List(Point(0, 0.5), Point(0, 1), Point(0, -1))

    def distance(p1: Point, p2: Point): Double = {
      val dx = p1.x - p2.x
      val dy = p1.y - p2.y
      Math.sqrt(dx * dx + dy * dy)
    }

    val clustering = Clustering.cluster("", elems, distance _, 2)
    val expectedValue = ClusteredStructure("", List(Cluster(List(Point(0, 0.5), Point(0, 1))), Cluster(List(Point(0, -1)))))

    assert(ClusteringTestUtils.compareClusterings(clustering, expectedValue))
  }

  test("Clustering correctness 2") {
    val elems = List(Point(0, 0.5), Point(0, 1), Point(0, -1))

    def distance(p1: Point, p2: Point): Double = {
      val dx = p1.x - p2.x
      val dy = p1.y - p2.y
      Math.sqrt(dx * dx + dy * dy)
    }

    val clustering = Clustering.cluster("", elems, distance _, 1)
    val expectedValue = ClusteredStructure("", List(Cluster(List(Point(0, 0.5), Point(0, 1), Point(0, -1)))))

    assert(ClusteringTestUtils.compareClusterings(clustering, expectedValue))
  }

  test("Clustering does not remove duplicates") {
    val elems = List(Point(0, 0), Point(1, 1), Point(0, 0), Point(1, 1))

    def distance(p1: Point, p2: Point): Double = {
      val dx = p1.x - p2.x
      val dy = p1.y - p2.y
      Math.sqrt(dx * dx + dy * dy)
    }

    val clustering = Clustering.cluster("", elems, distance _, 2)
    val expectedValue = ClusteredStructure("", List(Cluster(List(Point(0, 0), Point(0, 0))), Cluster(List(Point(1, 1), Point(1, 1)))))

    assert(ClusteringTestUtils.compareClusterings(clustering, expectedValue))
  }

  test("Multi ch.epfl.clustering") {
    val elems = List(Point(0, -0.4), Point(0, -1), Point(0, 0.6), Point(0, 1))

    def distance(p1: Point, p2: Point): Double = {
      val dx = p1.x - p2.x
      val dy = p1.y - p2.y
      Math.sqrt(dx * dx + dy * dy)
    }

    val clustering = Clustering.cluster[Point]("", elems, distance _, 1 to 3)
    val expectedValue = List(

      ClusteredStructure("", List(Cluster(List(Point(0, -0.4), Point(0, -1), Point(0, 0.6), Point(0, 1))))),
      ClusteredStructure("", List(Cluster(List(Point(0, -0.4), Point(0, -1))), Cluster(List(Point(0, 0.6), Point(0, 1))))),
      ClusteredStructure("", List(Cluster(List(Point(0, -0.4))), Cluster(List(Point(0, -1))), Cluster(List(Point(0, 0.6), Point(0, 1)))))
    )


    assert((clustering zip expectedValue).forall(c => ClusteringTestUtils.compareClusterings[Point](c._1, c._2)))
  }
}

case class Point(x: Double, y: Double)
