//  Hyperic plugin for vFabric/Pivotal Web Server
//  Copyright (C) 2012-2015, Pivotal Software, Inc
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License along
//  with this program; if not, write to the Free Software Foundation, Inc.,
//  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

package com.vmware.vfabric.hyperic.plugin.vfws;

import java.io.IOException;
import java.io.InputStream;

import java.util.jar.Attributes.Name;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.Iterator;

import org.hyperic.hq.plugin.netservices.HTTPCollector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;

public class VfwsCollector
    extends HTTPCollector {

    private static final Log log = LogFactory.getLog(VfwsCollector.class.getName());

    public void collect() {
        setMethod(METHOD_GET);
        super.collect();
    }

    private void parse(HttpResponse response) throws IOException {
        InputStream is = response.getEntity().getContent();

        if (is == null) {
            log.error("Unable to retrieve results. InputStream is null");
            return;
        }

        Manifest manifest = new Manifest(is);
        Attributes attributes = manifest.getMainAttributes();
        if (null == attributes) {
            log.error("Unable to parse results. No attributes found");
            return;
        }

        Iterator it = attributes.keySet().iterator();
        while (it.hasNext()) {
            Name key = (Name) it.next();
            if (key == null) {
                log.debug("Skipping null key");
                continue;
            }
            Object value = attributes.get(key);
            if (value.getClass() != String.class) {
                log.error("Attribute value not of class String");
                continue;
            }
            String keyName = key.toString();
            String val = (String) value;
            // BusyWorkers and IdleWorkers have u in the values
            if (keyName.contains("Workers")) {
                setWorkers(keyName, val);
            } else if (keyName.contains("StartTime")) {
                setStartTime(keyName, val);
            } else {
                setValue(keyName, val);
            }
        }
    }

    private void setWorkers(String key, String value) {
        // Remove the u from the end of the value
        setValue(key, value.substring(0, value.length() - 1));
    }

    private void setStartTime(String key, String value) {
        // StartTime is in micro seconds since epoch
        // there isn't a unit for that
        Long startTime = Long.valueOf(value) / 1000;
        setValue(key, (startTime.toString()));
    }

    protected void parseResults(HttpResponse response) {
        try {
            parse(response);
        } catch (IOException e) {
            log.error("Exception parsing: " + getURL(), e);
        }
    }

    protected void netstat() {
        // noop
    }
}
