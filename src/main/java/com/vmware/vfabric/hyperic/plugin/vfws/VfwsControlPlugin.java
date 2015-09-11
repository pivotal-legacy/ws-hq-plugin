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

//
//This file is heavily based on ApacheControlPlugin.java from apache-plugin
//

package com.vmware.vfabric.hyperic.plugin.vfws;

import org.hyperic.hq.product.ServerControlPlugin;
import org.hyperic.hq.product.PluginException;

import org.hyperic.util.config.ConfigResponse;

public class VfwsControlPlugin
    extends ServerControlPlugin {
    static final String DEFAULT_SCRIPT = "bin/httpdctl";
    static final String DEFAULT_PIDFILE = "logs/httpd.pid";

    public VfwsControlPlugin() {
        super();
        setPidFile(DEFAULT_PIDFILE);
        setControlProgram(DEFAULT_SCRIPT);
    }

    public boolean useSigar() {
        return true;
    }

    public void configure(ConfigResponse config) throws PluginException {
        super.configure(config);
        validateControlProgram(getTypeInfo().getName());
    }

    // Define control methods

    public void start() {
        doCommand("start");

        handleResult(STATE_STARTED);
    }

    // XXX: should we handle encrypted keys?
    public void startssl() {
        doCommand("startssl");

        handleResult(STATE_STARTED);
    }

    public void stop() {
        doCommand("stop");

        handleResult(STATE_STOPPED);
    }

    public void restart() {
        this.doCommand("restart");

        handleResult(STATE_STARTED);
    }

    public void graceful() {
        doCommand("graceful");

        handleResult(STATE_STARTED);
    }

    public void gracefulstop() {
        doCommand("gracefulstop");

        handleResult(STATE_STOPPED);
    }

    public void configtest() {
        // state does not change during configtest

        doCommand("configtest");
    }
}
