package org.sgdtk.io;

import org.sgdtk.struct.*;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Read a CRF++ file and convert to in-memory representation for feature extraction
 *
 * @author dpressel
 */
public class CRFXXTemplateLoader
{

    /**
     * Create a template loader
     */
    public CRFXXTemplateLoader()
    {
    }

    /**
     * Load the file into a {@link FeatureTemplate}
     *
     * @param file A template file
     * @return An in-memory extractor representation
     * @throws IOException
     */
    public FeatureTemplate load(File file) throws IOException
    {
        FeatureTemplate featureTemplate = new FeatureTemplate();
        BufferedReader reader = new BufferedReader(new FileReader(file));

        String line;
        for(int lineNum = 1 ; (line = reader.readLine()) != null; ++lineNum)
        {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#"))
                continue;

            // Otherwise better be a feature
            if (!line.startsWith("B") && !line.startsWith("U"))
            {
                throw new IOException("Invalid start of line: (" + lineNum + ")");
            }

            int nGram = line.startsWith("U") ? 1 : 2;
            int startIndex = line.indexOf(":") + 1;


            // Not a default feature, get to the good stuff
            if (startIndex != 0)
            {
                // This will include the rule name which is key for context disambiguation
                String ns = line.substring(0, startIndex).trim();
                line = line.substring(startIndex);
                featureTemplate.addExtractor(readExtractorRule(ns, line, nGram));
            }
            else
            {

                String ns = line.trim() + ":";
                FeatureExtractorPart part  = new ExtractPart(nGram, 0, 0);

                featureTemplate.addExtractor(new FeatureExtractor(ns, Arrays.asList(part)));
            }


        }
        return featureTemplate;
    }

    private FeatureExtractor readExtractorRule(String ns, String line, int nGram)
    {
        int relPos;
        int componentIndex;


        List<FeatureExtractorPart> parts = new ArrayList<FeatureExtractorPart>();
        StringBuffer sb = new StringBuffer();

        while (!line.isEmpty())
        {

            if (line.startsWith("%x["))
            {
                String sbStr = sb.toString();
                if (!sbStr.isEmpty())
                {
                    parts.add(new LiteralPart(sbStr));
                    sb = new StringBuffer();
                }
                String dims = line.substring(line.indexOf("[") + 1, line.indexOf("]"));

                String[] relPosIndex = dims.split(",");
                relPos = Integer.valueOf(relPosIndex[0]);
                componentIndex = relPosIndex.length == 2 ? Integer.valueOf(relPosIndex[1]) : 0;
                parts.add(new ExtractPart(nGram, relPos, componentIndex));
                int start = line.indexOf("]") + 1;
                if (start == line.length())
                {
                    line = "";
                }
                else
                {
                    line = line.substring(start);
                }
            }
            else
            {
                int nextVar = line.indexOf("%x");
                if (nextVar < 1)
                {
                    nextVar = line.length();
                }
                sb.append(line.substring(0, nextVar));
                line = line.substring(nextVar);
            }

        }
        String sbStr = sb.toString();
        if (!sbStr.isEmpty())
        {
            parts.add(new LiteralPart(sbStr));
        }


        return new FeatureExtractor(ns, parts);
    }

}
