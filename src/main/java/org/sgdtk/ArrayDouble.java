package org.sgdtk;


import java.util.Arrays;
import java.util.List;

/**
 * Would love to use some other library like Trove for this, but don't want to add big dependencies here, and we really
 * arent trying to do anything special here.  Not sure why this isnt built into standard Java at this point -- we need
 * a primitive collection (array) that is resizable, thats all.
 */
public class ArrayDouble
{
    // Dont access directly unless you are very careful
    public double[] v;
    int sz;

    public static int nextPowerOf2(int n)
    {
            n--;
            n |= n >> 1;
            n |= n >> 2;
            n |= n >> 4;
            n |= n >> 8;
            n |= n >> 16;
            n++;
            return n;
    }

    public ArrayDouble()
    {

    }

    public ArrayDouble(int sz)
    {
        this.sz = sz;
        v = new double[sz];
    }
    public ArrayDouble(int sz, double k)
    {
        this.sz = sz;
        v = new double[sz];
        Arrays.fill(v, 0, sz, k);

    }
    public ArrayDouble(ArrayDouble x)
    {
        this(x.v);
    }
    public ArrayDouble(double[] x)
    {
        this.sz = x.length;
        v = new double[sz];
        System.arraycopy(x, 0, v, 0, x.length);

    }


    public ArrayDouble(int sz, int k)
    {
        this.sz = sz;
        v = new double[sz];
        Arrays.fill(v, k);
    }


    public void reserve(int newSize)
    {
        if (newSize - sz > 0)
        {
            ensureCapacity(newSize - sz);
        }
    }

    public void resize(int newSize)
    {
        reserve(newSize);
        sz = newSize;
    }
    public void resize(int newSize, int k)
    {
        reserve(newSize);
        Arrays.fill(v, sz, newSize, k);
        sz = newSize;

    }

    public int size()
    {
        return sz;
    }

    public int capacity()
    {
        return v.length;
    }
    public boolean isEmpty()
    {
        return sz == 0;
    }

    public void pushBack(double aDouble)
    {
        ensureCapacity(1);
        v[sz] = aDouble;
        sz++;
    }

    public double popBack()
    {
        double last = v[sz - 1];
        if (sz > 0)
            sz--;
        return last;
    }

    public void pushBack(List<? extends Double> c)
    {
        int len = c.size();
        ensureCapacity(len);
        for (int i = 0; i < len; ++i, ++sz)
        {
            v[sz] = c.get(i);
        }

    }

    public void pushBack(double[] c, int len)
    {
        ensureCapacity(len);
        for (int i = 0; i < len; ++i, ++sz)
        {
            v[sz] = c[i];
        }
    }

    public void pushBack(ArrayDouble c)
    {
        pushBack(c.v, c.size());
    }

    private void ensureCapacity(int toAdd)
    {
        if (v == null)
        {
            v = new double[toAdd];
        }
        else if (toAdd + sz > v.length)
        {
            int next = nextPowerOf2(toAdd + sz);
            double[] vn = new double[next];
            System.arraycopy(v, 0, vn, 0, v.length);
            v = vn;
        }
    }

    public void copyTo(ArrayDouble target)
    {
        target.reserve(sz);
        target.clear();
        target.pushBack(this);
    }

    // Dont actually do any work
    public void clear()
    {
        sz = 0;
    }

    public void constant(double k)
    {
        Arrays.fill(v, 0, sz, k);
    }


    public double get(int index)
    {
        if (index >= sz)
        {
            throw new IndexOutOfBoundsException("" + index + " is beyond bounds " + sz);
        }
        return v[index];
    }

    // Same as get but NO bounds check
    public double at(int index)
    {
        return v[index];
    }

    public void set(double[] x)
    {
        reserve(x.length);
        System.arraycopy(x, 0, v, 0, x.length);
    }
    public void set(ArrayDouble x)
    {
        set(x.v);
    }

    public void set(int index, double element)
    {
        if (index >= sz)
        {
            throw new IndexOutOfBoundsException("" + index + " is beyond bounds " + sz);
        }
        v[index] = element;
    }

    public void scale(double scalar)
    {
        for (int i = 0; i < sz; ++i)
        {
            v[i] *= scalar;
        }
    }
    public void add(double scalar)
    {
        for (int i = 0; i < sz; ++i)
        {
            v[i] += scalar;
        }
    }

    public double addi(int index, double scalar)
    {
        v[index] += scalar;
        return v[index];
    }
    public double multi(int index, double scalar)
    {
        v[index] *= scalar;
        return v[index];
    }

    public void addn(double[] x)
    {
        int xsz = Math.min(x.length, sz);
        for (int i = 0; i < xsz; ++i)
        {
            v[i] += x[i];
        }
    }

    public void addn(ArrayDouble from)
    {
        int xsz = Math.min(size(), from.size());
        for (int i = 0; i < xsz; ++i)
        {
            v[i] += from.v[i];
        }
    }
    public void multn(double[] x)
    {
        int xsz = Math.min(x.length, sz);
        for (int i = 0; i < xsz; ++i)
        {
            v[i] *= x[i];
        }
    }
    public void multn(ArrayDouble from)
    {
        int xsz = Math.min(size(), from.size());
        for (int i = 0; i < xsz; ++i)
        {
            v[i] *= from.v[i];
        }
    }
}
