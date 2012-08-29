package com.vmware.vfabric.hyperic.plugin.vfws;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.hyperic.util.StringUtil;

public class Listen {

    private static final List<String>  PROTOCOLS = new ArrayList<String>();
    static {
        PROTOCOLS.add("http");
        PROTOCOLS.add("https");
    }
    private int _port = 0;
    private String _address = null;
    private String _proto = null;
    
    public Listen(String line) {
        String hostname, port;
        String[] exploded = StringUtil.explodeQuoted(line);
        if(exploded.length > 1) {
            // figure out if the extra is the protocol or if it's garbage.
            try {
                setProto(exploded[1]);
            } catch (IllegalArgumentException e) {
                //not http or https so ignore
            }
        }
        // ip/hostname with : port?
        String[] ent = exploded[0].split(":");
        if (ent.length > 1) {
            hostname = ent[0];
            port = ent[1];
        } else {
            // This will set both options to the same 
            // We'll figure out which one is valid next
            hostname = exploded[0];
            port = exploded[0];
        }
        try {
            setPort(port);
        } catch (IllegalArgumentException e) {
            // not a number so ignore
        }
        try {
            setAddress(hostname);
        } catch (IllegalArgumentException e) {
            // ignore
        }
    }
    
    public boolean isValid() {
        if (_port != 0 || _address != null) {
            return true;
        } else {
            return false;
        }
    }
    
    public void setPort(int port) {
        _port = port;
    }
    
    public void setPort(String port) {
        try {
            _port = Integer.parseInt(port);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException();
        }
    }
    
    public int getPort() {
        return _port;
    }
    
    public void setAddress(String address) {
        try {
            InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            // if it's unknown then we can't connect to it to get metrics later
            return;
        }
        _address = address; 
    }
    
    public String getAddress() {
        return _address;
    }
    
    public void setProto(String proto) {
        if(PROTOCOLS.contains(proto.toLowerCase())) {
            _proto = proto;
        } else {
            throw new IllegalArgumentException();
        }
    }
    
    public String getProto() {
        return _proto;
    }
}
