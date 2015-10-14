package org.sgdtk;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
public class ArrayDoubleTest
{


    @Test
    public void testCopyToEmpty() throws Exception
    {
        ArrayDouble ad = new ArrayDouble();
        for (int i = 0; i < 100; ++i)
        {
            ad.pushBack(i);
        }

        ArrayDouble copy = new ArrayDouble();

        ad.copyTo(copy);

        assertEquals(ad.size(), copy.size());
        for (int i = 0; i < copy.size(); ++i)
        {
            assertEquals((double)i, copy.get(i));
        }

    }
    @Test
    public void testResizeFromCtor() throws Exception
    {
        ArrayDouble ad = new ArrayDouble(10, 1);
        for (int i = 0; i < ad.size(); ++i)
        {
            assertEquals(ad.get(i), ad.at(i));
            assertEquals(ad.get(i), 1.0);
        }
        ad.reserve(100);
        assertEquals(ad.size(), 10);
        assertEquals(ad.capacity(), 128);

        ad.scale(0.7);

        for (int i = 0; i < ad.size(); ++i)
        {
            assertEquals(ad.get(i), ad.at(i));
            assertEquals(ad.get(i), 0.7, 0.0000001);
        }

        for (int i = ad.size(); i < ad.capacity(); ++i)
        {
            boolean threw = false;
            try
            {
                assertEquals(ad.at(i), 0.0);
                ad.get(i);

            }
            catch (RuntimeException runEx)
            {
                threw = true;
            }
            finally
            {
                assertTrue(threw);
            }
        }
        ad.clear();
        assertTrue(ad.isEmpty());
        assertEquals(ad.capacity(), 128);
        assertEquals(ad.size(), 0);

    }
    @Test
    public void testResizeFromResize() throws Exception
    {
        ArrayDouble ad = new ArrayDouble();
        ad.resize(10, 1);

        for (int i = 0; i < ad.size(); ++i)
        {
            assertEquals(ad.get(i), ad.at(i));
            assertEquals(ad.get(i), 1.0);
        }
        ad.reserve(100);
        assertEquals(ad.size(), 10);
        assertTrue(ad.capacity() > 100);

        ad.scale(0.7);

        for (int i = 0; i < ad.size(); ++i)
        {
            assertEquals(ad.get(i), ad.at(i));
            assertEquals(ad.get(i), 0.7, 0.0000001);
        }

        for (int i = ad.size(); i < ad.capacity(); ++i)
        {
            boolean threw = false;
            try
            {
                assertEquals(ad.at(i), 0.0);
                ad.get(i);

            }
            catch (RuntimeException runEx)
            {
                threw = true;
            }
            finally
            {
                assertTrue(threw);
            }
        }
        ad.clear();
        assertTrue(ad.isEmpty());
        assertTrue(ad.capacity() > 100);
        assertEquals(ad.size(), 0);

    }

    @Test
    public void testCapacity() throws Exception
    {
        ArrayDouble ad = new ArrayDouble();
        ad.reserve(1024);
        assertEquals(1024, ad.capacity());
        for (int i = 0; i < 1024; ++i)
        {
            ad.pushBack(10.0);
            assertEquals(ad.capacity(), 1024);
        }
        for (int i = 0; i < 10; ++i)
        {
            ad.pushBack(10.0);
            assertEquals(ad.capacity(), 2048);
        }


    }

    @Test
    public void setGetTest() throws Exception
    {
        ArrayDouble ad = new ArrayDouble();

        ad.reserve(1024);

        boolean threw = false;
        try
        {
            ad.set(100, 5);
        }
        catch (IndexOutOfBoundsException indout)
        {
            threw = true;
        }
        finally
        {
            assertTrue(threw);
        }

        for (int i = 0; i < 1024; ++i)
        {
            ad.pushBack(i);
        }

    }


    @Test
    public void rawSpeedTest() throws Exception
    {
        final double TIMES = 1000000;
        final int WIDTH = 2048;

        ArrayDouble ad = new ArrayDouble();
        ad.reserve(1024);
        double[] ad2 = new double[WIDTH];

        for (int j = 0; j < WIDTH; ++j)
        {
            double d = 1.0 / (j + 1);
            ad.pushBack(d);
            ad2[j] = d;
        }

        double t0 = System.currentTimeMillis();
        double acc = 0.;

        for (int i = 0; i < TIMES; ++i)
        {
            for (int j = 0; j < WIDTH; ++j)
            {
                acc += ad.at(j);
            }
            acc /= WIDTH;
        }
        double x = System.currentTimeMillis() - t0;
        System.out.println(x + "ms using ArrayDouble");

        t0 = System.currentTimeMillis();
        double acc2 = 0.;
        for (int i = 0; i < TIMES; ++i)
        {
            for (int j = 0; j < WIDTH; ++j)
            {
                acc2 += ad2[j];
            }
            acc2 /= ad2.length;
        }
        double x0 = System.currentTimeMillis() - t0;
        System.out.println(x0 + "ms using native array");

        System.out.println("Percent slower: " + ((x - x0)/x0) * 100 + "%");
        assertEquals(acc, acc2, 1e-6);

    }


}
