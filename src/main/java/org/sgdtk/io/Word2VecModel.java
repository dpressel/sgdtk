package org.sgdtk.io;


import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

// Matt Barta's port of my C++ file (which in turn is based on W2V source code)
public class Word2VecModel
{

    private static int MAX_W = 50;

    private final Long numWords;

    private final Long size;

    private final Map<String, Integer> vocab;

    private final float[][] vectors;

    private final static Charset CHARSET = Charset.forName("UTF-8");

    private final static ByteOrder ENDIAN = ByteOrder.LITTLE_ENDIAN;

    private static final long BUFFER = 1024 * 1024 * 1024;

    private static int bufferCount = 1;

   public final float[] NULLV;

    Word2VecModel(long words, long size)
    {
        this.numWords = words;
        this.size = size;
        this.vocab = new HashMap<String, Integer>(this.getNumWords().intValue());
        this.vectors = new float[this.getNumWords().intValue()][this.getSize().intValue()];

        this.NULLV = new float[this.getSize().intValue()];

    }

    public static Word2VecModel loadWord2VecModel(String file) throws
            IOException
    {
        Word2VecModel wvm;
        RandomAccessFile f = new RandomAccessFile(file, "r");
        FileChannel channel = f.getChannel();
        try
        {

            MappedByteBuffer rdr = channel.map(
                    FileChannel.MapMode.READ_ONLY, 0,Math.min(channel.size(), Integer.MAX_VALUE));
            rdr.order(ENDIAN);
            bufferCount = 1;

            StringBuilder sb = new StringBuilder();
            char c = (char) rdr.get();
            while (c != '\n')
            {
                sb.append(c);
                c = (char) rdr.get();
            }
            String firstLine = sb.toString();
            int index = firstLine.indexOf(' ');

            final long vocabSize = Long.
                    parseLong(firstLine.substring(0, index));
            final long layerSize = Long.parseLong(firstLine.
                    substring(index + 1));
            wvm = new Word2VecModel(vocabSize, layerSize);

            for (int b = 0; b < vocabSize; b++) //for each word
            {
                sb.setLength(0);
                c = (char) rdr.get();
                while (c != ' ' && sb.length() < Word2VecModel.MAX_W)
                {
                    if (c != '\n')
                    {
                        sb.append(c);
                    }
                    c = (char) rdr.get();
                }

                String str = sb.toString();
                int vsz = wvm.getVocab().size();
                //long offst = rdr.position() + ((bufferCount-1)*BUFFER);
                wvm.getVocab().put(str, vsz);

                float[] vec = wvm.vectors[vsz];
                for (int i = 0; i < vec.length; i++)
                {
                    vec[i] = rdr.getFloat();
                }

                if (rdr.position() > BUFFER)
                {

                    long pos = BUFFER*bufferCount;
                    final int newPosition = (int) (rdr.position() - BUFFER);
                    final long size = Math.
                            min(channel.size() - pos,
                                    Integer.MAX_VALUE);


                    rdr = channel.map(
                            FileChannel.MapMode.READ_ONLY, pos,
                            size);
                    rdr.order(ENDIAN);
                    rdr.position(newPosition);
                    bufferCount += 1;
                }

            }
        }
        finally
        {
            f.close();
            channel.close();
        }
        return wvm;
    }

    public float[] getVec(String word)
    {
        if (vocab.containsKey(word))
        {
            return this.vectors[vocab.get(word)];
        }
        return this.NULLV;
    }

    public Long getNumWords()
    {
        return numWords;
    }

    public Long getSize()
    {
        return size;
    }

    public Map<String, Integer> getVocab()
    {
        return vocab;
    }
}
