package com.beautyboss.slogen.rpc.registry;

import com.beautyboss.slogen.rpc.common.Constants;
import com.beautyboss.slogen.rpc.registry.zk.client.ZookeeperClient;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class RegistryConfig {
    public String addrs;
    public int timeout;
    public int sessionTimeout;
    public int retryPeriod;
    public boolean checkWhenStartup;
    public String zkclient;

    public RegistryConfig() {
    }

    public RegistryConfig(PropertiesConfiguration prop) {
        String[] hosts = prop.getStringArray(Constants.CONFIG_REGISTRY_ADDRS);
        Arrays.sort(hosts);
        addrs = StringUtils.join(hosts, ',');

        sessionTimeout = prop.getInt(Constants.CONFIG_REGISTRY_SESSION_TIMEOUT, Constants.DEFAULT_SESSION_TIMEOUT);
        if (sessionTimeout <= 0) {
            throw new IllegalArgumentException("sessionTimeout must > 0");
        }

        timeout = prop.getInt(Constants.CONFIG_REGISTRY_TIMEOUT, Constants.DEFAULT_TIMEOUT);
        if (timeout <= 0) {
            throw new IllegalArgumentException("timeout must > 0");
        }

        retryPeriod = prop.getInt(Constants.CONFIG_REGISTRY_RETRY_PERIOD, Constants.DEFAULT_RETRY_PERIOD);
        if (retryPeriod <= 0) {
            throw new IllegalArgumentException("retryPeriod must > 0");
        }

        checkWhenStartup = prop.getBoolean(Constants.CONFIG_REGISTRY_CHECK_WHEN_STARTUP, Constants.DEFAULT_CHECK_WHEN_STARTUP);

        zkclient = prop.getString(Constants.CONFIG_REGISTRY_ZKCLIENT, ZookeeperClient.ZK_CURATOR_CLIENT);
        if (!ZookeeperClient.ZK_CURATOR_CLIENT.equals(zkclient)
                && !ZookeeperClient.ZK_ZKCLIENT_CLIENT.equals(zkclient)) {
            throw new IllegalArgumentException("zkclient must be [curator|zkclient]");
        }
    }

    public String getKey() {
        return addrs;
    }

    public String toString() {
        return addrs;
    }
}
