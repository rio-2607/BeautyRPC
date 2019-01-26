package com.beautyboss.slogen.rpc.service.thrift;

import com.beautyboss.slogen.rpc.common.Constants;
import com.beautyboss.slogen.rpc.registry.RegistryConfig;
import com.beautyboss.slogen.rpc.util.JsonUtil;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class ThriftServiceConfig implements Cloneable {
    private static final Logger logger = LoggerFactory
            .getLogger(ThriftServiceConfig.class);

    private static ThriftServiceConfig config;

    /* public config */
    public String host = null;
    public int port;

    /* threaded pool config */
    public int minWorkerThreads;
    public int maxWorkerThreads;

    /* TFrametransport max length */
    public int tframeMaxLength;

    /* threaded selector config */
    public int selectorThreads;
    public int workerThreads;
    public int acceptQueueSizePerThread;

    /* registry config */
    public int rigistryDelay;
    public RegistryConfig regcfg;

    public synchronized static ThriftServiceConfig getConfig()
    {
        if (config != null) {
            return config;
        }

        PropertiesConfiguration prop;

        try {
            prop = new PropertiesConfiguration(Constants.DEFAULT_CONFIG_PROPERTIES);
        } catch (Exception e) {
            throw new RuntimeException("no thrift-rpc.properties file found", e);
        }

        config = new ThriftServiceConfig();

        config.regcfg = new RegistryConfig(prop);

        config.rigistryDelay = prop.getInt(Constants.CONFIG_REGISTRY_DELAY, Constants.DEFAULT_REGISTRY_DELAY);
        if (config.rigistryDelay < 0) {
            throw new IllegalArgumentException("rigistryDelay must be >= 0");
        }

        config.host = prop.getString(Constants.CONFIG_SERVICE_HOST);

        config.port = prop.getInt(Constants.CONFIG_SERVICE_PORT, Constants.DEFAULT_PORT);
        if (config.port < 0 || config.port > 65535) {
            throw new IllegalArgumentException("invalid port, must between (0,65536)");
        }

        config.minWorkerThreads = prop.getInt(Constants.CONFIG_MIN_WORKER_THREADS, Constants.DEFAULT_MIN_WORKER_THREADS);
        if (config.minWorkerThreads <= 0) {
            throw new IllegalArgumentException("minWorkerThreads must be > 0");
        }

        config.maxWorkerThreads = prop.getInt(Constants.CONFIG_MAX_WORKER_THREADS, Constants.DEFAULT_MAX_WORKER_THREADS);
        if (config.minWorkerThreads > config.maxWorkerThreads) {
            throw new IllegalArgumentException("maxWorkerThreads must be greater than maxWorkerThreads");
        }

        config.tframeMaxLength = prop.getInt(Constants.CONFIG_TFRAME_MAX_LENGTH, Constants.DEFAULT_TFRAME_MAX_LENGTH);
        if (config.tframeMaxLength <= 0) {
            throw new IllegalArgumentException("tframeMaxLength must be > 0");
        }

        config.selectorThreads = prop.getInt(Constants.CONFIG_SELECTOR_THREADS, Constants.DEFAULT_SELECTOR_THREADS);
        if (config.selectorThreads <= 0) {
            throw new IllegalArgumentException("selectorThreads must be > 0");
        }

        config.workerThreads = prop.getInt(Constants.CONFIG_WORKER_THREADS, Constants.DEFAULT_WORKER_THREADS);
        if (config.workerThreads <= 0) {
            throw new IllegalArgumentException("maxWorkerThreads must be > 0");
        }

        config.acceptQueueSizePerThread = prop.getInt(Constants.CONFIG_QUEUE_SIZE_PER_THREADS, Constants.DEFAULT_QUEUE_SIZE_PER_THREADS);
        if (config.acceptQueueSizePerThread <= 0) {
            throw new IllegalArgumentException("maxWorkerThreads must be > 0");
        }

        logger.info("thrift service use config:" + JsonUtil.toJson(config));

        return config;
    }
}
