package com.vmware.vfabric.hyperic.plugin.vfws;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.DaemonDetector;
import org.hyperic.hq.product.FileServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class VfwsServerDetector extends DaemonDetector 
    implements AutoServerDetector, FileServerDetector {
    
    private static final String _logCtx = VfwsServerDetector.class.getName();
    private final static Log _log = LogFactory.getLog(_logCtx);

    private static final String ARG_ROOTDIR = "-d";
    private static final String RESOURCE_TYPE = "vFabric Web Server";
    private static final String DEFAULT_BMX_PROTO = "http";
    private static final String DEFAULT_BMX_HOST = "localhost";
    private static final int DEFAULT_BMX_PORT = 80; 
    private static final String DEFAULT_BMX_PATH = "/bmx";
    private static final String QUERY_BMX = "?query=";
    private static final String SERVER_STATUS = "mod_bmx_status:Name=ServerStatus";
    private static final String VHOST_QUERY = "mod_bmx_vhost:";
    private static final String VHOST_SERVICE_TYPE = "Virtual Host";
    private static final String HTTPD_CONF = "/conf/httpd.conf";
    private static final String CONF_DIRECTIVE_LISTEN = "LISTEN";
    private static final String CONF_DIRECTIVE_INCLUDE = "INCLUDE";
    
    private static final List<String> _ptqlQueries = new ArrayList<String>();
    static {
        _ptqlQueries.add("State.Name.eq=httpd.prefork,State.Name.Pne=$1");
        _ptqlQueries.add("State.Name.eq=httpd.worker,State.Name.Pne=$1");
        if (isWin32()) {
            _ptqlQueries.add("State.Name.eq=httpd");
        }
    }
    
    public List<ServerResource> getServerResources(ConfigResponse platformConfig) throws PluginException {
        setPlatformConfig(platformConfig);
        List<ServerResource> servers = new ArrayList<ServerResource>();

        servers = getServers(getPtqlQueries());
        
        return servers;
    }

    private List<ServerResource> getServers(List<String> ptqlQueries) {
        List<ServerResource> servers = new ArrayList<ServerResource>();
        for(final Iterator<String> it=ptqlQueries.iterator(); it.hasNext(); ) {
            String ptql = (String)it.next();
            final long[] pids = getPids(ptql);
            if (null != pids && pids.length > 0) {
                for (int i=0; i<pids.length; i++) {
                    Long pid = pids[i];
                    String installPath = getInstallPath(pid);
                    if (null == installPath) {
                        _log.debug("Found pid " + pid + ", but couldn't identify installpath");
                        continue;
                    }
                    URL bmxUrl = findBmxUrl(installPath,  HTTPD_CONF);
                    if (bmxUrl == null) {
                        _log.debug("Parsing " + installPath + HTTPD_CONF + " failed to find " + 
                            "usable Listen directive.") ;
                        continue;
                    }
                    URL bmxQueryUrl = getBmxQueryUrl(bmxUrl,  QUERY_BMX + SERVER_STATUS);
                    BmxQuery query = new BmxQuery(bmxQueryUrl);
                    BmxResult result = query.getResult();
                    try {
                        result.parseToProperties();
                    } catch (IOException e) {
                        _log.debug("Unable to parse results", e);
                        continue;
                    }
                    Properties serverStatus = result.getProperties();
                    ServerResource server = createServerResource(installPath);
                    ConfigResponse cprop = new ConfigResponse();
                    String version = getVersion((String)serverStatus.get("ServerVersion"));
                    if (!version.equals(getTypeInfo().getVersion())) {
                        // Version not matched
                        continue;
                    }
                    cprop.setValue("version", version);
                    cprop.setValue("ServerVersion", (String)serverStatus.get("ServerVersion"));
                    server.setCustomProperties(cprop);
                    ConfigResponse productConfig = new ConfigResponse();
                    productConfig.setValue("process.query", getProcessQuery(ptql, installPath));
                    productConfig.setValue("protocol", bmxUrl.getProtocol());
                    productConfig.setValue("port", bmxUrl.getPort());
                    productConfig.setValue("hostname", bmxUrl.getHost());
                    productConfig.setValue("path", bmxUrl.getPath() + QUERY_BMX + SERVER_STATUS);
                    setProductConfig(server, productConfig);
                    // sets a default Measurement Config property with no values
                    setMeasurementConfig(server, new ConfigResponse());
                    String instanceName = getInstanceName(installPath);
                    server.setName(getPlatformName() + " " + RESOURCE_TYPE + " " + version + " " + instanceName);
                    servers.add(server);
                }
            }
        }
        return servers;
    }
    
    private String getVersion(String versionString) {
        String[] ent = versionString.split(" ");
        for(int i = 0; i < ent.length; i++) {
            if(ent[i].contains("vFabric/")) {
                return ent[i].split("/")[1].substring(0,3);
            }
        }
        return null;
    }

    private URL getBmxQueryUrl(URL url, String path) {
        try {
            URL newUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), 
                        url.getPath() + path);
            return newUrl;
        } catch (MalformedURLException e) {
            // return old url?
            return url;
        }
    }
    
    protected List discoverServices(ConfigResponse config)
        throws PluginException {
        List<ServiceResource> services = new ArrayList<ServiceResource>();
        try {
            String proto = config.getValue("protocol");
            String hostname = config.getValue("hostname");
            int port = Integer.parseInt(config.getValue("port"));
            String path = DEFAULT_BMX_PATH + QUERY_BMX + VHOST_QUERY + "*";
            URL bmxUrl = new URL(proto, hostname, port, path);
            BmxQuery query = new BmxQuery(bmxUrl);
            BmxResult result = query.getResult();
            List<String> names = result.parseForNames();
            for(Iterator<String> it = names.iterator(); it.hasNext();) {
                String name = it.next();
                String type = getTypeInfo().getName() + " " + VHOST_SERVICE_TYPE;
                ServiceResource service = new ServiceResource();
                String[] ent = name.split(",");
                if(ent[0].equals("Type=since-start")) {
                    String host = ent[1].split("=")[1];
                    String servicePort = ent[2].split("=")[1];
                    path = DEFAULT_BMX_PATH + QUERY_BMX + VHOST_QUERY + name;
                    ConfigResponse cprops = new ConfigResponse();
                    cprops.setValue("protocol", proto);
                    cprops.setValue("hostname", hostname);
                    cprops.setValue("port", port);
                    cprops.setValue("path", path);
                    service.setProductConfig(cprops);
                    service.setMeasurementConfig();
                    service.setType(type);
                    service.setServiceName(host + ":" + servicePort);
                    services.add(service);
                }
            }
        } catch (Exception e) {
            _log.debug("Exception" + e,e);
            return null;
        }
        return services;
    }
    
    private String getInstanceName(String installPath) {
        File name = new File(installPath);
        return name.getName();
    }
    
    private String getProcessQuery(String ptql, String installPath) {
        return ptql + ",Args.*.eq=" + ARG_ROOTDIR + 
                      ",Args.*.eq=" + installPath;
    }
    
    private URL findBmxUrl(String installPath, String filename) {
        URL url = null;
        String proto = DEFAULT_BMX_PROTO; 
        String host = DEFAULT_BMX_HOST;
        int port = DEFAULT_BMX_PORT;
        String path = DEFAULT_BMX_PATH;
        
        List<Listen> listens = getListens(installPath, filename);
        for(Iterator<Listen> it = listens.iterator(); it.hasNext();) {
            Listen listen = it.next();
            if(listen.getPort() != 0) {
                port = listen.getPort();
                _log.debug("Port set to " + port);
            }
            if(listen.getAddress() != null) {
                host = listen.getAddress();
                _log.debug("host set to " + host);
            }
            if(listen.getProto() != null) {
                proto = listen.getProto();
                _log.debug("Proto set to " + proto);
            }
            try {
                _log.debug("Trying to make a URL from " + proto + ", " + host + ", " + port + ", " + path);
                url = new URL(proto, host, port, path);
                BmxQuery query = new BmxQuery(url);
                query.getResult();
                return url;
            } catch (MalformedURLException e) {
                _log.error(e,e);
            }
        }
        return url;
    }
    
    private List<Listen> getListens(String installPath, String filename) {
        List<Listen> listens = new ArrayList<Listen>();
        List<String> config = parseConfigForListen(installPath, filename);
        for(Iterator<String> it = config.iterator(); it.hasNext();) {
            Listen listen = new Listen((String) it.next()); 
            if (listen.isValid()) {
                listens.add(listen);
            }
        }
        return listens;
    }

    private String getInstallPath(Long pid) {
        String[] args = getProcArgs(pid);
        String last = null;
        for(int i=0; i<args.length; i++) {
            // look for -d and use next arg as installpath
            // some installs have multiple -d, return the last
            if (ARG_ROOTDIR.equals(args[i])) {
                last = args[i+1];
            }
        }
        return last;
    }
    
    // Mostly borrowed this from apache-plugin. 
    // TODO This needs cleaning up
    private static List<String> parseConfigForListen(String installPath, String filename) {
        File file = new File(installPath + "/" + filename);
        List<String> config = new ArrayList<String>();
        String line;
        BufferedReader reader = null;
        
        if(!file.exists()) {
            _log.debug(file.getAbsolutePath() + " doesn't exist");
            return config;
        }        

        try {
            reader = new BufferedReader(new FileReader(file));
            while ((line = reader.readLine()) != null) {
                if (line.length() == 0) {
                    continue;
                }
                char chr = line.charAt(0);
                if ((chr == '#') || (chr == '<') ||
                    Character.isWhitespace(chr))
                {
                    continue; //only looking at top-level
                }
                int ix = line.indexOf('#');
                if (ix != -1) {
                    line = line.substring(0, ix);
                }
                line = line.trim();
                String[] ent = StringUtil.explodeQuoted(line);
                if (CONF_DIRECTIVE_LISTEN.equals(ent[0].toUpperCase())) {
                    if (ent.length > 2) {
                        // there may be more than one option so combine them
                        String value = "";
                        for (int i = 1; i<ent.length; i++) {
                            if (null != ent[i]) {
                                value += " " + ent[i];
                            }
                        }
                        config.add(value);
                    } else {
                        config.add(ent[1]);
                    }
                } else if (CONF_DIRECTIVE_INCLUDE.equals(ent[0].toUpperCase())) {
                    List <String> includedConf = parseConfigForListen(installPath, ent[1]);
                    if(!includedConf.isEmpty()) {
                        config.addAll(includedConf);
                    }
                }
            }
        } catch (IOException e) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {}
            }
        }
        return config;
    }

    private List<String> getPtqlQueries() {
        return _ptqlQueries;
    }
}
