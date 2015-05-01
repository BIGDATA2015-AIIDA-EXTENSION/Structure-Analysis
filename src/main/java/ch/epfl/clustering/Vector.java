package ch.epfl.clustering;

import java.util.List;

/**
 * Created by lukas on 28/04/15.
 */
public final class Vector {
    private double[] values;

    public Vector(double... values) {
        this.values = values.clone();
    }



    public double norm() {
        double sum = 0.0;

        for(int i=0; i < values.length; i++) {
            sum += (values[i] * values[i]);
        }
        return Math.sqrt(sum);
    }

    public Vector add(Vector v, double times) {
        if(this.values.length != v.values.length) throw new IllegalArgumentException();

        double[] vals = new double[values.length];

        for(int i=0; i < vals.length; i++) {
            vals[i] = values[i] + times*v.values[i];
        }
        return new Vector(vals);
    }

    public Vector diff(Vector v) {
        if(this.values.length != v.values.length) throw new IllegalArgumentException();

        double[] vals = new double[values.length];

        for(int i=0; i < vals.length; i++) {
            vals[i] = values[i] - v.values[i];
        }
        return new Vector(vals);
    }

    public static Vector crossProduct(Vector u, Vector v) {
        if(u.values.length != 3 || v.values.length != 3) throw new IllegalArgumentException();
        double u1 = u.values[0];
        double u2 = u.values[1];
        double u3 = u.values[2];
        double v1 = v.values[0];
        double v2 = v.values[1];
        double v3 = v.values[2];

        return new Vector(u2*v3 - u3*v2, u3*v1 - u1*v3, u1*v2 - u2*v1);
    }

    public static double dotProduct(Vector u, Vector v) {
        if(u.values.length != v.values.length) throw new IllegalArgumentException();

        double sum = 0.0;
        for(int i=0; i < u.values.length; i++) {
            sum += u.values[i]*v.values[i];
        }

        return sum;
    }


    public static Vector mean(List<Vector> vectors) {
        if(vectors.isEmpty()) throw new IllegalArgumentException();

        double[] v = new double[vectors.get(0).values.length];
        for(Vector vec : vectors) {
            for(int i=0; i < v.length; i++){
                v[i] += vec.values[i];
            }
        }

        for(int i=0; i < v.length; i++){
            v[i] /= vectors.size();
        }

        return new Vector(v);
    }

    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        for(int i=0; i < values.length; i++) {
            str.append(values[i]);
            if(i != values.length-1) str.append(" ");
        }

        return str.toString();
    }

    public static double distance(Vector v1, Vector v2) {
        if(v1.values.length != v2.values.length) throw new IllegalArgumentException();

        double sum = 0.0;

        for(int i=0; i < v1.values.length; i++) {
            sum += (v1.values[i] - v2.values[i])*(v1.values[i] - v2.values[i]);
        }
        return Math.sqrt(sum);
    }

}
