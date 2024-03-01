package com.wyona.katie.ai.models;

/**
 *
 */
public class FloatVector {

    float[] values;

    /**
     *
     */
    public FloatVector(int dimension) {
        values = new float[dimension];
        for (int i = 0; i < dimension; i++) {
            values[i] = 0;
        }
    }

    /**
     *
     */
    public FloatVector(float[] values) {
        this.values = new float[values.length];
        for (int i = 0; i < getDimension(); i++) {
            this.values[i] = values[i];
        }
    }

    /**
     *
     */
    public FloatVector clone() {
        FloatVector w = new FloatVector(values.length);
        for (int i = 0; i < values.length; i++) {
            w.set(i, values[i]);
        }
        return w;
    }

    /**
     *
     */
    public void set(int index, float value) {
        values[index] = value;
    }

    /**
     *
     */
    public float[] getValues() {
        return values;
    }

    /**
     *
     */
    public int getDimension() {
        return values.length;
    }

    /**
     *
     */
    public double getLength() {
        double length = 0.0;
        for (int i = 0; i < values.length; i++) {
            length += values[i] * values[i];
        }
        return Math.sqrt(length);
    }

    /**
     *
     */
    public void add(FloatVector w) {
        for (int i = 0; i < values.length; i++) {
            values[i] += w.getValues()[i];
        }
    }

    /**
     *
     */
    public void scale(float factor) {
        for (int i = 0; i < values.length; i++) {
            values[i] = factor * values[i];
        }
    }

    /**
     *
     */
    public void normalize() {
        float length = (float) getLength();
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i] / length;
        }
    }

    /**
     *
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("(");
        for (int i = 0; i < values.length; i++) {
            s.append("" + values[i]);
            if (i < values.length - 1) {
                s.append(" , ");
            }
        }
        s.append(")");
        return s.toString();
    }
}
