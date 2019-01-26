package com.beautyboss.slogen.rpc.client.thrift;

import com.beautyboss.slogen.rpc.service.Server;
import com.beautyboss.slogen.rpc.pool.ConnPool;
import com.beautyboss.slogen.rpc.pool.ConnPoolConfig;
import com.beautyboss.slogen.rpc.pool.ConnWrapObject;
import org.apache.thrift.transport.TSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class ThriftTSocketPool implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ThriftTSocketPool.class);

    private final ConcurrentHashMap<String, ConnPool<TSocket>> lives = new ConcurrentHashMap<>();
    private ConnPoolConfig config;

    public ThriftTSocketPool(ConnPoolConfig config) {
        this.config = config;
    }

    public ConnWrapObject<TSocket> getResource(String key) throws Exception
    {
        ConnPool<TSocket> pool = lives.get(key);

        if (pool == null) {
            lives.putIfAbsent(key, new ConnPool<TSocket>(
                    new ThriftTSocketFactory(), Server.valueOf(key), config));
            pool = lives.get(key);
        }

        return pool.borrowObject();
    }

    public void returnResource(String key, ConnWrapObject<TSocket> resource)
    {
        TSocket obj = resource.getObject();
        if (!obj.isOpen()) {
            return;
        }

        try {
            ConnPool<TSocket> pool = lives.get(key);
            if (pool != null) {
                pool.returnObject(resource);
            }
        } catch (Exception e) {
            logger.error("Could not return the resource to the pool", e);
        }
    }

    public void returnBrokenResource(String key, ConnWrapObject<TSocket> resource)
    {
        try {
            ConnPool<TSocket> pool = lives.get(key);
            if (pool != null) {
                pool.invalidateObject(resource);
            }
        } catch (Exception e) {
            logger.error("Could not return broken resource to the pool", e);
        }
    }

    @Override
    public synchronized void close()
    {
        for (Map.Entry<String, ConnPool<TSocket>> entry : lives.entrySet()) {
            entry.getValue().close();
        }
    }

    public void clear(String key)
    {
        ConnPool<TSocket> pool = lives.get(key);
        if (pool != null) {
            pool.clear();
        }
    }

    public Map<String, ConnPool<TSocket>> getLives()
    {
        return lives;
    }
}

