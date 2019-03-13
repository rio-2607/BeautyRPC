package com.beautyboss.slogen.rpc.service;

import org.apache.commons.lang.StringUtils;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class Server {
    private String host;
    private int port;

    public Server(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static Server valueOf(String key) {
        String[] tmp = StringUtils.split(key, ':');
        return new Server(tmp[0], Integer.parseInt(tmp[1]));
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String toString() {
        return host + ":" + port;
    }
}
