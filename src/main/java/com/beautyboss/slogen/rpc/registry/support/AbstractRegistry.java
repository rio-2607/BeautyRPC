package com.beautyboss.slogen.rpc.registry.support;

import com.beautyboss.slogen.rpc.service.Service;
import com.beautyboss.slogen.rpc.common.ConcurrentHashSet;
import com.beautyboss.slogen.rpc.registry.NotifyListener;
import com.beautyboss.slogen.rpc.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public abstract class AbstractRegistry implements Registry {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Set<Service> registered = new ConcurrentHashSet<>();

    private final ConcurrentMap<String, Set<NotifyListener>> subscribed = new ConcurrentHashMap<>();

    /* init by user */
    protected String hosts;

    public Set<Service> getRegistered()
    {
        return registered;
    }

    public Map<String, Set<NotifyListener>> getSubscribed()
    {
        return subscribed;
    }

    public void register(Service service)
    {
        if (service == null) {
            throw new IllegalArgumentException("register service == null");
        }
        logger.info("Register: " + service);

        registered.add(service);
    }

    public void unregister(Service service)
    {
        if (service == null) {
            throw new IllegalArgumentException("unregister service == null");
        }
        logger.info("Unregister: " + service);
        registered.remove(service);
    }

    public void subscribe(String service, NotifyListener listener)
    {
        if (service == null) {
            throw new IllegalArgumentException("subscribe service == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("subscribe listener == null");
        }
        logger.info("Subscribe: " + service);
        Set<NotifyListener> listeners = subscribed.get(service);
        if (listeners == null) {
            subscribed.putIfAbsent(service, new ConcurrentHashSet<NotifyListener>());
            listeners = subscribed.get(service);
        }
        listeners.add(listener);
    }

    public void unsubscribe(String service, NotifyListener listener)
    {
        if (service == null) {
            throw new IllegalArgumentException("unsubscribe service == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("unsubscribe listener == null");
        }
        logger.info("Unsubscribe: " + service);
        Set<NotifyListener> listeners = subscribed.get(service);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    protected void recover() throws Exception
    {
        // register
        Set<Service> recoverRegistered = new HashSet<>(getRegistered());
        if (!recoverRegistered.isEmpty()) {
            logger.info("Recover register service " + recoverRegistered);
            for (Service service : recoverRegistered) {
                register(service);
            }
        }
        // subscribe
        Map<String, Set<NotifyListener>> recoverSubscribed = new HashMap<>(getSubscribed());
        if (!recoverSubscribed.isEmpty()) {
            logger.info("Recover subscribe service " + recoverSubscribed.keySet());
            for (Map.Entry<String, Set<NotifyListener>> entry : recoverSubscribed.entrySet()) {
                String service = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    subscribe(service, listener);
                }
            }
        }
    }

    protected void notify(List<Service> services)
    {
        if (services == null || services.isEmpty()) return;

        for (Map.Entry<String, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
            String service = entry.getKey();

            Set<NotifyListener> listeners = entry.getValue();
            if (listeners != null) {
                for (NotifyListener listener : listeners) {
                    try {
                        notify(service, listener, services);
                    } catch (Throwable t) {
                        logger.error(
                                "Failed to notify registry event, services: " + services + ", cause: " + t.getMessage(),
                                t);
                    }
                }
            }
        }
    }

    protected void notify(String service, NotifyListener listener, List<Service> services)
    {
        if (service == null) {
            throw new IllegalArgumentException("notify service == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("notify listener == null");
        }
        if (services == null || services.size() == 0) {
            logger.warn("Ignore empty notify services for subscribe service " + service);
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Notify services for subscribe service " + service + ", services: " + services);
        }

        listener.notify(services);
    }

    public void destroy()
    {
        if (logger.isInfoEnabled()) {
            logger.info("Destroy registry");
        }
        Set<Service> destroyRegistered = new HashSet<>(getRegistered());
        if (!destroyRegistered.isEmpty()) {
            for (Service service : new HashSet<>(getRegistered())) {
                try {
                    unregister(service);
                    if (logger.isInfoEnabled()) {
                        logger.info("Destroy unregister service " + service);
                    }
                } catch (Throwable t) {
                    logger.warn("Failed to unregister service " + service + " to registry " + getHosts()
                            + " on destroy, cause: " + t.getMessage(), t);
                }

            }
        }
        Map<String, Set<NotifyListener>> destroySubscribed = new HashMap<>(getSubscribed());
        if (!destroySubscribed.isEmpty()) {
            for (Map.Entry<String, Set<NotifyListener>> entry : destroySubscribed.entrySet()) {
                String service = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    try {
                        unsubscribe(service, listener);
                        if (logger.isInfoEnabled()) {
                            logger.info("Destroy unsubscribe service " + service);
                        }
                    } catch (Throwable t) {
                        logger.warn("Failed to unsubscribe service " + service + " to registry " + getHosts()
                                + " on destroy, cause: " + t.getMessage(), t);
                    }
                }
            }
        }
    }

    public String getHosts()
    {
        return hosts;
    }

    public void setHosts(String hosts)
    {
        this.hosts = hosts;
    }
}
