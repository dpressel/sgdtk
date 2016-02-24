package org.sgdtk.io;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Class to read in a configuration for a neural net for training from a model
 */
public class JsonConfigReader implements ConfigReader
{
    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Override
    public Config read(InputStream inputStream) throws IOException
    {
        Map<String, Object> top = OBJECT_MAPPER.readValue(inputStream, Map.class);
        String netName = (String)top.get("name");
        Config props = new Config();
        props.setName(netName);

        Map<String, Object> map = (Map<String, Object>)top.get("learner");
        props.setLearner(map);

        map = (Map<String, Object>)top.get("model");
        props.setModel(map);
        return props;

    }

    @Override
    public Config read(File file) throws IOException
    {
        FileInputStream fis = new FileInputStream(file);
        Config props = read(fis);
        fis.close();
        return props;
    }
}
