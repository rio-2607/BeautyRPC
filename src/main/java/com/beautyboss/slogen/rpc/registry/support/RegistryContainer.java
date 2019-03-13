package com.beautyboss.slogen.rpc.registry.support;

import com.beautyboss.slogen.rpc.registry.Registry;
import com.beautyboss.slogen.rpc.registry.RegistryConfig;
import com.beautyboss.slogen.rpc.registry.zk.ZookeeperRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class RegistryContainer {
    private static final Logger logger = LoggerFactory.getLogger(RegistryContainer.class);

    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final Map<String, Registry> REGISTRYS = new HashMap<>();

    public static void destroyAll() {
        LOCK.lock();
        logger.info("Close all registries " + REGISTRYS.values());
        try {
            for (Registry registry : REGISTRYS.values()) {
                try {
                    registry.destroy();
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                }
            }
            REGISTRYS.clear();
        } finally {
            // 释放锁
            LOCK.unlock();
        }
    }

    public static Registry getRegistry(RegistryConfig config) {
        if (config.addrs == null || config.addrs.isEmpty()) {
            return null;
        }

        LOCK.lock();
        try {
            Registry registry = REGISTRYS.get(config.getKey());
            if (registry != null) {
                return registry;
            }

            ZookeeperRegistry zregistry = new ZookeeperRegistry();
            zregistry.setHosts(config.addrs);
            zregistry.setTimeout(config.timeout);
            zregistry.setSessionTimeout(config.sessionTimeout);
            zregistry.setRetryPeriod(config.retryPeriod);
            zregistry.setCheckWhenStartup(config.checkWhenStartup);
            zregistry.setClient(config.zkclient);
            zregistry.init();

            REGISTRYS.put(config.getKey(), zregistry);
            return zregistry;
        } finally {
            LOCK.unlock();
        }
    }

    public static Registry createRegistry(RegistryConfig config) {
        if (config.addrs == null || config.addrs.isEmpty()) {
            return null;
        }

        ZookeeperRegistry zregistry = new ZookeeperRegistry();
        zregistry.setHosts(config.addrs);
        zregistry.setTimeout(config.timeout);
        zregistry.setSessionTimeout(config.sessionTimeout);
        zregistry.setRetryPeriod(config.retryPeriod);
        zregistry.setCheckWhenStartup(config.checkWhenStartup);
        zregistry.setClient(config.zkclient);
        zregistry.init();

        return zregistry;
    }
}
