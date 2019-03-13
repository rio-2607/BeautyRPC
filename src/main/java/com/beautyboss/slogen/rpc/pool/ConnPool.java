package com.beautyboss.slogen.rpc.pool;

import com.beautyboss.slogen.rpc.service.Server;
import org.apache.commons.pool2.PoolUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class ConnPool<T> {
    private static final Logger logger = LoggerFactory
            .getLogger(ConnPool.class);

    // Configuration attributes
    private final Server _server;
    private ConnPoolConfig _config;
    private ConcurrentLinkedQueue<ConnWrapObject<T>> _pool = null;
    private ConnFactory<T> _factory = null;
    private volatile boolean closed = false;

    public ConnPool(ConnFactory<T> factory, Server server,
                    ConnPoolConfig config) {
        if (factory == null) {
            throw new IllegalArgumentException("factory may not be null");
        }

        if (server == null) {
            throw new IllegalArgumentException("server may not be null");
        }

        _server = server;
        _factory = factory;
        _config = config;

        _pool = new ConcurrentLinkedQueue<>();
    }

    private ConnWrapObject<T> wrap(T obj)
    {
        return new ConnWrapObject<>(obj);
    }

    public ConnWrapObject<T> borrowObject() throws Exception
    {
        assertOpen();

        ConnWrapObject<T> p = null;

        boolean create;

        while (p == null) {
            create = false;
            p = _pool.poll();
            if (p == null) {
                p = wrap(_factory.makeObject(_server));
                if (p != null) {
                    create = true;
                }
            }

            if (p != null) {
                if (!p.allocate()) {
                    p = null;
                }
            }
            if (p != null) {
                try {
                    _factory.activateObject(p.getObject());
                } catch (Exception e) {
                    try {
                        destroy(p);
                    } catch (Exception e1) {
                        logger.error("destroy object error: ", e);
                        // Ignore - activation failure is more important
                    }
                    p = null;
                    if (create) {
                        NoSuchElementException nsee = new NoSuchElementException(
                                "Unable to activate object");
                        nsee.initCause(e);
                        throw nsee;
                    }
                }
                if (p != null
                        && (isTestOnBorrow() || create && isTestOnCreate())) {
                    boolean validate = false;
                    Throwable validationThrowable = null;
                    try {
                        validate = _factory.validateObject(p.getObject());
                    } catch (Throwable t) {
                        PoolUtils.checkRethrow(t);
                        validationThrowable = t;
                    }
                    if (!validate) {
                        try {
                            destroy(p);
                        } catch (Exception e) {
                            logger.error("destroy object error: ", e);
                            // Ignore - validation failure is more important
                        }
                        p = null;
                        if (create) {
                            NoSuchElementException nsee = new NoSuchElementException(
                                    "Unable to validate object");
                            nsee.initCause(validationThrowable);
                            throw nsee;
                        }
                    }
                }
            }
        }

        return p;
    }

    public void returnObject(ConnWrapObject<T> obj) throws Exception
    {
        try {
            addObjectToPool(obj, true);
        } catch (Exception e) {
            if (_factory != null) {
                try {
                    _factory.destroyObject(obj.getObject());
                } catch (Exception e2) {
                    logger.error("destroy object error: ", e2);
                    // swallowed
                }
            }
        }
    }

    public void invalidateObject(ConnWrapObject<T> obj) throws Exception
    {
        if (_factory != null) {
            _factory.destroyObject(obj.getObject());
        }
    }

    public synchronized void clear()
    {
        List<ConnWrapObject<T>> toDestroy = new ArrayList<>(_pool);

        _pool.clear();

        for (int i = 0; i < toDestroy.size(); i++) {
            destroy(toDestroy.get(i));
        }
    }

    private void destroy(ConnWrapObject<T> p)
    {
        try {
            _factory.destroyObject(p.getObject());
        } catch (Exception e) {
            logger.error("destroy object error: ", e);
            // ignore error, keep destroying the rest
        }
    }

    private void addObjectToPool(ConnWrapObject<T> t, boolean decrementNumActive)
            throws Exception
    {
        T obj = t.getObject();
        boolean success = true;
        if (isTestOnReturn() && !(_factory.validateObject(obj))) {
            success = false;
        } else {
            _factory.passivateObject(obj);
        }

        boolean shouldDestroy = !success;
        int maxIdle = getMaxIdle();
        // Add instance to pool if there is room and it has passed validation
        // (if testOnreturn is set)
        if (isClosed()) {
            shouldDestroy = true;
        } else {
            if ((maxIdle >= 0) && (_pool.size() >= maxIdle)) {
                shouldDestroy = true;
            } else if (success) {
                if (!t.deallocate()) {
                    throw new IllegalStateException(
                            "Object has already been returned to this pool or is invalid");
                }
                _pool.add(t);
            }
        }

        // Destroy the instance if necessary
        if (shouldDestroy) {
            try {
                _factory.destroyObject(obj);
            } catch (Exception e) {
                logger.error("destroy object error: ", e);
                // ignored
            }
        }
    }

    public synchronized void close()
    {
        if (isClosed()) {
            return;
        }

        closed = true;
        clear();
    }

    public void addObject() throws Exception
    {
        assertOpen();
        if (_factory == null) {
            throw new IllegalStateException(
                    "Cannot add objects without a factory.");
        }
        T obj = _factory.makeObject(_server);
        try {
            assertOpen();
            addObjectToPool(wrap(obj), false);
        } catch (IllegalStateException ex) { // Pool closed
            try {
                _factory.destroyObject(obj);
            } catch (Exception ex2) {
                logger.error("destroy object error: ", ex2);
                // swallow
            }
            throw ex;
        }
    }

    private boolean isClosed()
    {
        return closed;
    }

    private void assertOpen() throws IllegalStateException
    {
        if (isClosed()) {
            throw new IllegalStateException("Pool not open");
        }
    }

    public int getNumIdle()
    {
        return _pool.size();
    }

    public Server getRpcServer()
    {
        return _server;
    }

    public int getMaxIdle()
    {
        return _config.maxIdle;
    }

    public boolean isTestOnCreate()
    {
        return _config.testOnCreate;
    }

    public boolean isTestOnBorrow()
    {
        return _config.testOnBorrow;
    }

    public boolean isTestOnReturn()
    {
        return _config.testOnReturn;
    }
}
