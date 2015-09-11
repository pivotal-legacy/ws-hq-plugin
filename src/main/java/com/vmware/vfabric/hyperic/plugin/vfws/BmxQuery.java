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
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;

// TODO: Add a MeasurementPlugin to use this class for metrics instead of Collector
public class BmxQuery {

    private static final String _logCtx = BmxQuery.class.getName();
    private final static Log _log = LogFactory.getLog(_logCtx);

    private URL _url;
    private BmxResult _result;

    public BmxQuery(URL url) {
        setURL(url);
    }

    public void setURL(URL url) {
        _url = url;
    }

    public URL getURL() {
        return _url;
    }

    public BmxResult getResult() {
        doQuery();
        return _result;
    }

    private void doQuery() {
        HttpClient client = new DefaultHttpClient();

        HttpRequestBase method;
        try {
            _log.debug("Getting URL " + getURL().toURI().toString());
            method = new HttpGet(getURL().toURI());
        } catch (URISyntaxException e) {
            _log.debug(e, e);
            return;
        }

        method.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, true); // follow
                                                                              // redirects

        try {
            HttpResponse response = client.execute(method);
            _result = new BmxResult(response);
            _log.debug("Got " + response.getStatusLine().getStatusCode() + " from " +
                       getURL().toString());
        } catch (IOException e) {
            _log.debug(e, e);
        }

    }
}
