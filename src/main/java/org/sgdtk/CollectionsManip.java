package org.sgdtk;

import java.util.HashMap;
import java.util.Map;

/**
 * Low level utilities just for the library
 *
 * These functions do not perform range checking or array size sanity checks, be careful!
 * TODO: switch from Math.exp(-x) to Bottou's polynomial approximation version
 * @author dpressel
 */
public class CollectionsManip
{
    /**
     * This operation is simple but tedious, and distracts from a readable flow
     * Totally unsafe, make sure you call it right
     * @param dst Destination
     * @param src Source
     */
    public static void addInplace(double[] dst, double [] src)
    {
        for (int i = 0; i < dst.length; ++i)
        {
            dst[i] += src[i];
        }
    }

    /**
     * Add src1 and src2 and store in dst
     * @param dst Destination
     * @param src1 first thing to add
     * @param src2 addee
     */
    public static void copyAdd(double[] dst, double[] src1, double [] src2)
    {
        System.arraycopy(src1, 0, dst, 0, src1.length);
        CollectionsManip.addInplace(dst, src2);
    }

    /**
     * Scale the array inplace
     * @param ary The array to scale
     * @param scalar The scalar
     */
    public static void scaleInplace(double[] ary, double scalar)
    {
        for (int i = 0; i < ary.length; ++i)
        {
            ary[i] *= scalar;
        }
    }

    /**
     * Scale a 2D array inplace
     * @param ary The ary
     * @param scalar The scalar
     */
    public static void scaleInplace(double[][] ary, double scalar)
    {
        for (int i = 0; i < ary.length; ++i)
        {
            for (int j = 0; j < ary.length; ++j)
            {
                ary[i][j] *= scalar;
            }
        }
    }

    /**
     * Dot product of a and b
     * @param a vector
     * @param b vector
     * @return dot product
     */
    public static double dot(double[] a, double [] b)
    {
        double acc = 0.;
        for (int i = 0; i < a.length; ++i)
        {
            acc += a[i] * b[i];
        }
        return acc;
    }


    /**
     * Scaled log sum
     * @param v
     */
    public static double logSum(double[] v)
    {
        double m = v[0];
        for (int i = 1; i < v.length; ++i)
        {
            m = Math.max(m, v[i]);
        }
        double s = 0.;
        for (int i = 0; i < v.length; ++i)
        {
            s += Math.exp(-(m - v[i]));
        }
        return m + Math.log(s);
    }

    /**
     * Derivative log sum
     * @param g
     * @param v
     * @param r
     */
    public static void dLogSum(double g, double[] v, double [] r)
    {
        double m = v[0];
        for (int i = 0; i < v.length; ++i)
        {
            m = Math.max(m, v[i]);
        }
        double z = 0.;
        for (int i = 0; i < v.length; ++i)
        {
            r[i] = Math.exp(-(m - v[i]));
            z += r[i];
        }
        for (int i = 0; i < v.length; ++i)
        {
            r[i] *= g / z;
        }
    }

    /**
     * Invert map
     * @param map
     * @param <X>
     * @param <Y>
     * @return inverted
     */
    public synchronized static <X, Y> Map<Y, X> inverted(Map<X, Y> map) {
        Map<Y, X> flipped = new HashMap<Y, X>();
        for(X x: map.keySet())
        {
            flipped.put(map.get(x), x);
        }
        return flipped;
    }

    // Java 8 has this, so does Apache Commons, but assume LCD here
    public static String join(String[] str, String joinStr)
    {
        StringBuffer buffer = new StringBuffer();
        int i = 0;
        for (; i < str.length - 1; ++i)
        {
            buffer.append(str[i]);
            buffer.append(joinStr);
        }
        buffer.append(str[i]);
        return buffer.toString();
    }

    // Exists in Java 8 and Apache Commons, but assume LCD here
    public static <K, T> T getOrDefault(Map<K, T> ftable, K key, T def)
    {
        T count = ftable.get(key);
        if (count == null)
        {
            count = def;
        }
        return count;
    }


}
