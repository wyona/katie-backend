package com.wyona.katie.models;

/**
 * Vector with byte values, which means the values are in the range of -127 to 127
 */
public class ByteVector implements Vector {

    byte[] values;

    /**
     *
     */
    public ByteVector(int dimension) {
        values = new byte[dimension];
        for (int i = 0; i < dimension; i++) {
            values[i] = 0;
        }
    }

    /**
     *
     */
    public ByteVector(byte[] values) {
        this.values = new byte[values.length];
        for (int i = 0; i < getDimension(); i++) {
            this.values[i] = values[i];
        }
    }

    /**
     *
     */
    public ByteVector clone() {
        ByteVector w = new ByteVector(values.length);
        for (int i = 0; i < values.length; i++) {
            w.set(i, values[i]);
        }
        return w;
    }

    /**
     *
     */
    public void set(int index, byte value) {
        values[index] = value;
    }

    /**
     *
     */
    public byte[] getValues() {
        return values;
    }

    /**
     * @see com.wyona.katie.models.Vector#getDimension()
     */
    public int getDimension() {
        return values.length;
    }

    /**
     * @see com.wyona.katie.models.Vector#add(Vector)
     */
    public void add(Vector w) {
        ByteVector _w = (ByteVector)w;
        for (int i = 0; i < values.length; i++) {
            values[i] += _w.getValues()[i];
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
