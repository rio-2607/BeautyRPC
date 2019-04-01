package com.beautyboss.slogen.rpc.registry.zk;

import com.beautyboss.slogen.rpc.service.Service;
import com.beautyboss.slogen.rpc.common.Constants;
import com.beautyboss.slogen.rpc.exceptions.RpcException;
import com.beautyboss.slogen.rpc.registry.NotifyListener;
import com.beautyboss.slogen.rpc.registry.support.FailbackRegistry;
import com.beautyboss.slogen.rpc.registry.zk.client.ChildListener;
import com.beautyboss.slogen.rpc.registry.zk.client.StateListener;
import com.beautyboss.slogen.rpc.registry.zk.client.ZookeeperClient;
import com.beautyboss.slogen.rpc.registry.zk.client.impl.CuratorZookeeperClient;
import com.beautyboss.slogen.rpc.registry.zk.client.impl.ZkclientZookeeperClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class ZookeeperRegistry extends FailbackRegistry {
    private final ConcurrentMap<String, ConcurrentMap<NotifyListener, ChildListener>> zkListeners = new ConcurrentHashMap<>();

    /* init by framework */
    private AtomicBoolean inited = new AtomicBoolean();
    private AtomicBoolean destroyed = new AtomicBoolean();
    private ZookeeperClient zkClient;

    /* init by user */
    private String root = Constants.DEFAULT_GROUP;
    private boolean ephemeral = Constants.DEFAULT_EPHEMERAL_NODE;
    private int timeout = Constants.DEFAULT_TIMEOUT;
    private int sessionTimeout = Constants.DEFAULT_SESSION_TIMEOUT;
    private String client = ZookeeperClient.ZK_CURATOR_CLIENT;

    public void init() {
        if (getHosts() == null) {
            throw new IllegalStateException("registry address == null");
        }

        if (!inited.compareAndSet(false, true)) {
            return;
        }

        logger.debug("init registry");

        super.init();

        String group = getRoot();
        if (!group.startsWith(Constants.PATH_SEPARATOR)) {
            group = Constants.PATH_SEPARATOR + group;
        }

        root = group;
        if (ZookeeperClient.ZK_CURATOR_CLIENT.equals(client)) {
            zkClient = new CuratorZookeeperClient(getHosts(), getSessionTimeout(), getTimeout());
        } else if (ZookeeperClient.ZK_ZKCLIENT_CLIENT.equals(client)) {
            zkClient = new ZkclientZookeeperClient(getHosts(), getSessionTimeout(), getTimeout());
        } else {
            throw new IllegalStateException("invalid zkclient config");
        }

        zkClient.addStateListener(new StateListener() {
            private boolean firstConnected = true;

            public void stateChanged(int state) {
                logger.debug("zk state change " + state);
                if (state == CONNECTED) {
                    logger.info("state change to connected " + firstConnected);
                }

                if (state == RECONNECTED) {
                    try {
                        recover();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return zkClient.isConnected();
    }

    @Override
    public void destroy() {
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }

        super.destroy();
        try {
            zkClient.close();
        } catch (Exception e) {
            logger.warn("Failed to close zookeeper client " + getHosts() + ", cause: " + e.getMessage(), e);
        }
    }

    protected void doRegister(Service service) {
        try {
            zkClient.create(toUrlPath(service), isEphemeral());
        } catch (Throwable e) {
            throw new RpcException("Failed to register " + service + " to zookeeper " + getHosts() + ", cause: "
                    + e.getMessage(), e);
        }
    }

    protected void doUnregister(Service service) {
        try {
            zkClient.delete(toUrlPath(service));
        } catch (Throwable e) {
            throw new RpcException("Failed to unregister " + service + " to zookeeper " + getHosts() + ", cause: "
                    + e.getMessage(), e);
        }
    }

    protected void doSubscribe(final String service, final NotifyListener listener) {
        try {
            List<Service> services = new ArrayList<Service>();
            String path = toSubscribePath(service);
            ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.get(service);
            if (listeners == null) {
                zkListeners.putIfAbsent(service, new ConcurrentHashMap<>());
                listeners = zkListeners.get(service);
            }
            ChildListener zkListener = listeners.get(listener);
            if (zkListener == null) {
                listeners.putIfAbsent(listener, (parentPath, currentChilds) -> {
                    logger.debug("path is updated: " + parentPath + "," + currentChilds);
                    ZookeeperRegistry.this.notify(service, listener, toUrlsWithoutEmpty(service, currentChilds));
                });
                zkListener = listeners.get(listener);
            }
            zkClient.create(path, false);
            List<String> children = zkClient.addChildListener(path, zkListener);
            if (children != null) {
                services.addAll(toUrlsWithoutEmpty(service, children));
            }

            notify(service, listener, services);

        } catch (Throwable e) {
            throw new RpcException("Failed to subscribe " + service + " to zookeeper " + getHosts() + ", cause: "
                    + e.getMessage(), e);
        }
    }

    protected void doUnsubscribe(String service, NotifyListener listener) {
        ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.get(service);
        if (listeners != null) {
            ChildListener zkListener = listeners.get(listener);
            if (zkListener != null) {
                zkClient.removeChildListener(toSubscribePath(service), zkListener);
            }
        }
    }

    private String toRootDir() {
        if (root.equals(Constants.PATH_SEPARATOR)) {
            return root;
        }
        return root + Constants.PATH_SEPARATOR;
    }

    private String toSubscribePath(String service) {
        return toRootDir() + service + Constants.PATH_SEPARATOR + Service.PROVIDER_CATEGORY;
    }

    private String toServicePath(Service service) {
        return toRootDir() + Service.encode(service.name);
    }

    private String toCategoryPath(Service service) {
        return toServicePath(service) + Constants.PATH_SEPARATOR + service.category;
    }

    private String toUrlPath(Service service) {
        return toCategoryPath(service) + Constants.PATH_SEPARATOR + Service.encode(service.toString());
    }

    private List<Service> toUrlsWithoutEmpty(String service, List<String> providers) {
        List<Service> services = new ArrayList<Service>();
        if (providers != null && providers.size() > 0) {
            for (String provider : providers) {
                provider = Service.decode(provider);
                Service s = Service.valueOf(provider);
                if (s != null) {
                    services.add(s);
                } else {
                    logger.error("invalid provider: " + provider);
                }
            }
        }
        return services;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public boolean isEphemeral() {
        return ephemeral;
    }
    // public void setEphemeral(boolean ephemeral)
    // {
    // this.ephemeral = ephemeral;
    // }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }
}
