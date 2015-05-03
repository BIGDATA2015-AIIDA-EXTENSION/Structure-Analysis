package ch.epfl.clustering;

import java.util.Collections;
import java.util.List;

/**
 * Created by lukas on 27/04/15.
 */
public class Clustering {
    private List<Cluster> clusters;
    private Vector dataMean;
    private int nbElems;

    public Clustering(List<Cluster> clusters, Vector mean, int nbElems) {
        this.clusters = clusters;
        this.dataMean = mean;
        this.nbElems = nbElems;
    }

    public double clusterNumberMetrics(){

        if(clusters.size() == 1 | nbElems == clusters.size()) return 0.0;

        double b = 0.0;
        for(Cluster c : clusters) {
            b += c.elems.size() * c.clusterMean().diff(dataMean).norm();
        }

        double w = 0.0;
        for(Cluster c : clusters) {
            w += c.variation();
        }

        return (b / (clusters.size() - 1)) / (w / (nbElems - clusters.size()));
    }

    public List<Cluster> clusters(){
        return clusters;
    }

    public double metrics(){
        double sum = 0.0;
        int count = 0;
        for(Cluster c : clusters) {
            if(c.elems.size() > 3) {
                count++;
                double d = c.metrics();
                sum += d;
            }
        }
        return count != 0 ? sum/count : 0.0;
    }

    public static class Cluster {
        private List<Vector> elems;
        private int id;

        public Cluster(List<Vector> elems, int id) {
            this.elems = elems;
            this.id = id;
        }

        public String toString(){
            StringBuilder str = new StringBuilder();
            for(int i=0; i < elems.size(); i++) {
                str.append(id);
                str.append(" ");
                str.append(elems.get(i));

                if(i != elems.size()-1) str.append(" ");
            }
            return str.toString();
        }

        public Vector clusterMean(){
            return Vector.mean(elems);
        }

        public double variation() {
            Vector mean = clusterMean();
            double var = 0.0;
            for(Vector v : elems) {
                var += v.diff(mean).norm();
            }
            return var;
        }

        public double metrics() {
            if(elems.size() < 4) throw new IllegalArgumentException();

            int nbMeasures = 10;
            double sum = 0.0;
            for(int i=0; i < nbMeasures; i++) {
                Collections.shuffle(elems);
                Plane plane = new Plane(elems.get(0), elems.get(1), elems.get(2));
                for(int j=3; j < elems.size(); j++) {
                    double d = plane.distance(elems.get(j));
                    sum += d/(elems.size()-3);
                }
            }
            return sum/nbMeasures;
        }

        private class Plane {
            private Vector p;
            private Vector normal;
            private double d;

            public Plane(Vector p1, Vector p2, Vector p3) {
                this.p = p1;
                this.normal = Vector.crossProduct(p1.diff(p2), p1.diff(p3));
                this.d = - Vector.dotProduct(p, normal);
            }

            public double distance(Vector p) {
                return normal.norm() != 0 ? Math.abs(Vector.dotProduct(normal, p)+d) / normal.norm() : 0.0;
            }

        }
    }
}
