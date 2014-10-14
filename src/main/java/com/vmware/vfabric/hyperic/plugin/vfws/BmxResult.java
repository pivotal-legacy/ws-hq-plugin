package com.vmware.vfabric.hyperic.plugin.vfws;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;

public class BmxResult {
    private static final String _logCtx = BmxQuery.class.getName();
    private final static Log _log = LogFactory.getLog(_logCtx);
    Properties props = null;

    HttpResponse _response = null;

    public BmxResult(HttpResponse response) {
        _response = response;
    }

    public Properties getProperties() {
        return props;
    }

    public List<String> parseForNames() throws IllegalStateException, IOException {
        InputStream is = _response.getEntity().getContent();
        InputStreamReader reader = new InputStreamReader(is);
        BufferedReader bufferedReader = new BufferedReader(reader);
        List<String> list = new ArrayList<String>();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.startsWith("Name:")) {
                String[] ent = line.split(":");
                list.add((String) ent[2]);
            }
        }
        return list;
    }

    public Properties parseToProperties() throws IOException {
        InputStream is = _response.getEntity().getContent();
        props = new Properties();

        Manifest manifest = new Manifest(is);
        Attributes attributes = manifest.getMainAttributes();
        if (null == attributes) {
            _log.error("Unable to parse results. No attributes found");
            return null;
        }

        for (Iterator<Object> it = attributes.keySet().iterator(); it.hasNext();) {
            Name key = (Name) it.next();
            if (key == null) {
                _log.debug("Skipping null key");
                continue;
            }
            Object value = attributes.get(key);
            if (value.getClass() != String.class) {
                _log.error("Attribute value not of class String");
                continue;
            }
            String keyName = key.toString();
            String val = (String) value;
            props.put(keyName, val);
        }
        return props;
    }

}
