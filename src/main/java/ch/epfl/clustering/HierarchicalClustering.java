package ch.epfl.clustering;

import java.util.*;

/**
 * Created by lukas on 27/04/15.
 */
public class HierarchicalClustering {

    private Map<Vector, Integer> elemInCluster;
    private Map<Integer, Cluster_> clusters;
    private List<DistHolder> distance;
    private int nbElems;
    private Vector mean;

    public HierarchicalClustering(List<Vector> elems) {
        this.nbElems = elems.size();
        this.mean = Vector.mean(elems);
        elemInCluster = new HashMap<Vector, Integer>();
        clusters = new HashMap<Integer, Cluster_>();
        this.distance = new ArrayList<DistHolder>(nbElems*(nbElems-1)/2);
        for(int i=0; i < elems.size(); i++) {
            elemInCluster.put(elems.get(i), i);
            clusters.put(i, new Cluster_(i, elems.get(i)));

            for(int j = i+1; j < elems.size(); j++) {
                distance.add(new DistHolder(elems.get(i), elems.get(j)));
            }
        }
        Collections.sort(distance);
    }

    private List<Cluster_> clusterize(int nbClusters) {
        int nbIter = nbElems-nbClusters;
        System.out.println(nbIter);
        int index = 0;
        while(nbIter > 0) {
            while(distance.get(index).sameCluster()) index++;

            Cluster_ cluster1 = distance.get(index).getElem1Cluster();
            Cluster_ cluster2 = distance.get(index).getElem2Cluster();

            cluster1.fusionCluster(cluster2);
            for(Vector e : cluster2.elems()) {
                elemInCluster.put(e, cluster1.id());
            }
            clusters.remove(cluster2.id());

            index++;
            nbIter--;
        }
        return new ArrayList<Cluster_>(clusters.values());
    }

    public Clustering compute(int nbClusters) {
        List<Cluster_> cl = clusterize(nbClusters);
        List<Clustering.Cluster> retVal = new ArrayList<Clustering.Cluster>();
        for(Cluster_ c : cl) {
            retVal.add(new Clustering.Cluster(c.elems(), c.id));
        }

        return new Clustering(retVal, mean, nbElems);
    }





    private class DistHolder implements Comparable<DistHolder> {
        private Vector elem1;
        private Vector elem2;
        private double distance;

        public DistHolder(Vector elem1, Vector elem2) {
            this.elem1 = elem1;
            this.elem2 = elem2;
            this.distance = Vector.distance(elem1, elem2);
        }

        public boolean sameCluster(){
            long e1ClusterId = elemInCluster.get(elem1);
            long e2ClusterId = elemInCluster.get(elem2);
            return e1ClusterId == e2ClusterId;
        }


        @Override
        public int compareTo(DistHolder o) {
            if(distance < o.distance) return -1;
            else if(distance > o.distance) return 1;
            else return 0;
        }

        public Cluster_ getElem1Cluster(){
            return clusters.get(elemInCluster.get(elem1));
        }

        public Cluster_ getElem2Cluster(){
            return clusters.get(elemInCluster.get(elem2));
        }

        public String toString() {
            return elem1.toString()+" "+elem2.toString()+" "+distance;
        }
    }

    public class Cluster_ {
        private List<Vector> elems;
        private int id;

        public Cluster_(int id, Vector elem) {
            this.id = id;
            elems = new ArrayList<Vector>();
            elems.add(elem);
        }

        public int id(){
            return id;
        }

        public List<Vector> elems(){
            return elems;
        }

        public void fusionCluster(Cluster_ c) {
            List<Vector> newElems = c.elems;
            elems.addAll(newElems);
        }


    }

}




