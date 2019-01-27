package com.beautyboss.slogen.rpc.client.thrift;

import com.beautyboss.slogen.rpc.common.Constants;
import com.beautyboss.slogen.rpc.config.ConfigHelper;
import com.beautyboss.slogen.rpc.pool.ConnPoolConfig;
import com.beautyboss.slogen.rpc.registry.RegistryConfig;
import com.beautyboss.slogen.rpc.util.JsonUtil;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class ThriftClientConfig implements Cloneable {
    private static final Logger logger = LoggerFactory.getLogger(ThriftClientConfig.class);

    public int port;
    public int retries;
    public int socketTimeout;
    public ConnPoolConfig poolConfig;

    /* registry config */
    public RegistryConfig regcfg;

    /* static service addrs */
    public int heartbeatPeriod;

    public ThriftClientConfig()
    {
        this(Constants.DEFAULT_CONFIG_PROPERTIES);
    }

    public ThriftClientConfig(InputStream inputStream)
    {

        PropertiesConfiguration prop = new PropertiesConfiguration();

        try {
            prop.load(inputStream);
            prop.setDelimiterParsingDisabled(false);
        } catch (ConfigurationException e) {
            throw new RuntimeException("no rpc config file found", e);
        }

        initConfig(prop);
    }

    public ThriftClientConfig(String configFile)
    {
        PropertiesConfiguration prop;

        try {
            prop = ConfigHelper.getProperties();
            prop.setDelimiterParsingDisabled(false);
        } catch (Exception e) {
            throw new RuntimeException("no rpc config file found", e);
        }

        initConfig(prop);
    }

    public void initConfig(PropertiesConfiguration prop)
    {
        poolConfig = new ConnPoolConfig(prop);
        regcfg = new RegistryConfig(prop);

        port = prop.getInt(Constants.CONFIG_SERVICE_PORT, Constants.DEFAULT_PORT);
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("invalid port, must between (0,65536)");
        }

        retries = prop.getInt(Constants.CONFIG_CLIENT_RETRIES, Constants.DEFAULT_RETRIES);
        if (retries < 0) {
            throw new IllegalArgumentException("retries must >= 0");
        }

        socketTimeout = prop.getInt(Constants.CONFIG_CLIENT_TIMEOUT, Constants.DEFAULT_CLIENT_TIMEOUT);
        if (socketTimeout <= 0) {
            throw new IllegalArgumentException("socketTimeout must > 0");
        }

        heartbeatPeriod = prop.getInt(Constants.CONFIG_HEARTBEAT_PERIOD, Constants.DEFAULT_HEARTBEAT_PERIOD);
        if (heartbeatPeriod < 0) {
            throw new IllegalArgumentException("heartbeatPeriod must >= 0");
        }

        logger.info("thrift client use config: " + JsonUtil.toJson(this));
    }
}
