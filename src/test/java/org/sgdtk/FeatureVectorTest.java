package org.sgdtk;

import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class FeatureVectorTest
{


    @Test
    public void testDenseSerialization() throws Exception
    {
        DenseVectorN dv = new DenseVectorN(1000);
        for (int i = 0; i < 1000; ++i)
        {
            dv.set(i, Math.random());
        }

        FeatureVector fv = new FeatureVector(1, dv);
        UnsafeMemory mem = fv.denseSerialize(null);
        FeatureVector fv2 = FeatureVector.deserializeDense(mem.getBuffer());

        assertEquals(fv.getY(), fv2.getY());
        DenseVectorN dv2 = (DenseVectorN)fv2.getX();

        for (int i = 0; i < 1000; ++i)
        {
            assertEquals(dv2.getX().get(i), dv.getX().get(i), 1e-6);
        }
    }

    @Test
    public void testDenseSerializationToDisk() throws Exception
    {
        DenseVectorN dv = new DenseVectorN(1000);
        for (int i = 0; i < 1000; ++i)
        {
            dv.set(i, Math.random());
        }

        FeatureVector fv = new FeatureVector(1, dv);
        UnsafeMemory memory = fv.denseSerialize(null);

        File tempFile = File.createTempFile("dense", "ser");
        tempFile.deleteOnExit();

        RandomAccessFile randomAccessFile = new RandomAccessFile(tempFile, "rw");
        // Write bytes out
        long inBuffer = memory.getPos();
        randomAccessFile.writeLong(inBuffer);
        randomAccessFile.write(memory.getBuffer(), 0, (int) inBuffer);
        randomAccessFile.close();

        randomAccessFile = new RandomAccessFile(tempFile, "r");
        int recordLength = (int) randomAccessFile.readLong();
        byte [] b = new byte[recordLength];
        randomAccessFile.read(b, 0, recordLength);
        randomAccessFile.close();

        assertEquals(recordLength + 8, tempFile.length());

        FeatureVector fv2 = FeatureVector.deserializeDense(b);

        assertEquals(fv.getY(), fv2.getY());
        DenseVectorN dv2 = (DenseVectorN)fv2.getX();

        for (int i = 0; i < 1000; ++i)
        {
            assertEquals(dv2.getX().get(i), dv.getX().get(i), 1e-6);
        }
    }


    @Test
    public void testSparseSerialization() throws Exception
    {

        SparseVectorN sv = new SparseVectorN();

        for (int i = 0; i < 100; ++i)
        {
            int idx = (int)Math.random() * 999;
            double value = Math.random();
            sv.add(new Offset(idx, value));
        }
        sv.organize();

        FeatureVector fv = new FeatureVector(1, sv);

        UnsafeMemory mem = fv.sparseSerialize(null);
        FeatureVector fv2 = FeatureVector.deserializeSparse(mem.getBuffer());

        assertEquals(fv.getY(), fv2.getY());

        List<Offset> sv1 = sv.getNonZeroOffsets();
        List<Offset> sv2 = fv2.getNonZeroOffsets();
        assertEquals(sv1.size(), sv2.size());
        for (int i = 0; i < sv1.size(); ++i)
        {
            Offset o1 = sv1.get(i);
            Offset o2 = sv2.get(i);
            assertEquals(o1.index, o2.index);
            assertEquals(o1.value, o2.value, 1e-6);
        }


    }
    @Test
    public void testSparseSerializationToDisk() throws Exception
    {

        SparseVectorN sv = new SparseVectorN();

        for (int i = 0; i < 100; ++i)
        {
            int idx = (int)Math.random() * 999;
            double value = Math.random();
            sv.add(new Offset(idx, value));
        }
        sv.organize();

        FeatureVector fv = new FeatureVector(1, sv);

        UnsafeMemory memory = fv.sparseSerialize(null);


        File tempFile = File.createTempFile("sparse", "ser");
        tempFile.deleteOnExit();

        RandomAccessFile randomAccessFile = new RandomAccessFile(tempFile, "rw");
        // Write bytes out
        long inBuffer = memory.getPos();
        randomAccessFile.writeLong(inBuffer);
        randomAccessFile.write(memory.getBuffer(), 0, (int) inBuffer);
        randomAccessFile.close();

        assertEquals(inBuffer + 8, tempFile.length());


        randomAccessFile = new RandomAccessFile(tempFile, "r");
        int recordLength = (int) randomAccessFile.readLong();
        byte [] b = new byte[recordLength];
        randomAccessFile.read(b, 0, recordLength);
        randomAccessFile.close();


        FeatureVector fv2 = FeatureVector.deserializeSparse(b);

        assertEquals(fv.getY(), fv2.getY());

        List<Offset> sv1 = sv.getNonZeroOffsets();
        List<Offset> sv2 = fv2.getNonZeroOffsets();
        assertEquals(sv1.size(), sv2.size());
        for (int i = 0; i < sv1.size(); ++i)
        {
            Offset o1 = sv1.get(i);
            Offset o2 = sv2.get(i);
            assertEquals(o1.index, o2.index);
            assertEquals(o1.value, o2.value, 1e-6);
        }


    }


}
