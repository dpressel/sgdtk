
package org.sgdtk;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Unsafe memory access from Martin Thompson.  You must have a buffer big enough to handle the data
 *
 * This is re-appropriated from Martin Thompson's awesome article, with some tiny mods for usability
 * See <a href="http://mechanical-sympathy.blogspot.com/2012/07/native-cc-like-performance-for-java.html></a>
 * <p>
 * Thanks, dude, you're the man...
 *
 *
 */
public class UnsafeMemory
{
    private static final Unsafe unsafe;
    static
    {
        try
        {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe)field.get(null);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static final long byteArrayOffset = unsafe.arrayBaseOffset(byte[].class);
    private static final long longArrayOffset = unsafe.arrayBaseOffset(long[].class);
    private static final long doubleArrayOffset = unsafe.arrayBaseOffset(double[].class);

    public static final int SIZE_OF_BOOLEAN = 1;
    public static final int SIZE_OF_INT = 4;
    public static final int SIZE_OF_LONG = 8;
    public static final int SIZE_OF_DOUBLE = 8;

    private int pos = 0;
    private final byte[] buffer;

    public UnsafeMemory(int sz)
    {
        buffer = new byte[sz];
    }

    public UnsafeMemory(byte[] buffer)
    {
        this.buffer = buffer;
    }

    public void reset()
    {
        this.pos = 0;
    }

    public byte [] getBuffer()
    {
        return buffer;
    }
    public int getPos()
    {
        return pos;
    }

    public void putBoolean(final boolean value)
    {
        unsafe.putBoolean(buffer, byteArrayOffset + pos, value);
        pos += SIZE_OF_BOOLEAN;
    }

    public boolean getBoolean()
    {
        boolean value = unsafe.getBoolean(buffer, byteArrayOffset + pos);
        pos += SIZE_OF_BOOLEAN;

        return value;
    }

    public void putInt(final int value)
    {
        unsafe.putInt(buffer, byteArrayOffset + pos, value);
        pos += SIZE_OF_INT;
    }

    public int getInt()
    {
        int value = unsafe.getInt(buffer, byteArrayOffset + pos);
        pos += SIZE_OF_INT;

        return value;
    }

    public void putLong(final long value)
    {
        unsafe.putLong(buffer, byteArrayOffset + pos, value);
        pos += SIZE_OF_LONG;
    }

    public long getLong()
    {
        long value = unsafe.getLong(buffer, byteArrayOffset + pos);
        pos += SIZE_OF_LONG;

        return value;
    }

    public void putDouble(final double value)
    {
        unsafe.putDouble(buffer, byteArrayOffset + pos, value);
        pos += SIZE_OF_DOUBLE;
    }

    public double getDouble()
    {
        double value = unsafe.getDouble(buffer, byteArrayOffset + pos);
        pos += SIZE_OF_DOUBLE;

        return value;
    }

    public void putLongArray(final long[] values)
    {
        putInt(values.length);

        long bytesToCopy = values.length << 3;
        unsafe.copyMemory(values, longArrayOffset,
                buffer, byteArrayOffset + pos,
                bytesToCopy);
        pos += bytesToCopy;
    }

    public long[] getLongArray()
    {
        int arraySize = getInt();
        long[] values = new long[arraySize];

        long bytesToCopy = values.length << 3;
        unsafe.copyMemory(buffer, byteArrayOffset + pos,
                values, longArrayOffset,
                bytesToCopy);
        pos += bytesToCopy;

        return values;
    }

    public void putDoubleArray(final double[] values)
    {
        putInt(values.length);

        long bytesToCopy = values.length << 3;
        unsafe.copyMemory(values, doubleArrayOffset,
                buffer, byteArrayOffset + pos,
                bytesToCopy);
        pos += bytesToCopy;
    }

    public double[] getDoubleArray()
    {
        int arraySize = getInt();
        double[] values = new double[arraySize];

        long bytesToCopy = values.length << 3;
        unsafe.copyMemory(buffer, byteArrayOffset + pos,
                values, doubleArrayOffset,
                bytesToCopy);
        pos += bytesToCopy;

        return values;
    }
}
