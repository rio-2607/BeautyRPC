package com.beautyboss.slogen.rpc.registry.zk.client.impl;

import com.beautyboss.slogen.rpc.registry.zk.client.AbstractZookeeperClient;
import com.beautyboss.slogen.rpc.registry.zk.client.ChildListener;
import com.beautyboss.slogen.rpc.registry.zk.client.StateListener;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.CuratorFrameworkFactory.Builder;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.WatchedEvent;

import java.util.List;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class CuratorZookeeperClient extends AbstractZookeeperClient<CuratorWatcher> {
    private final CuratorFramework client;

    public CuratorFramework getClient()
    {
        return client;
    }

    public CuratorZookeeperClient(String hosts, int sessionTimeout, int connTimeout) {
        super(hosts);
        Builder builder = CuratorFrameworkFactory.builder()
                .connectString(hosts)
                .retryPolicy(new RetryNTimes(Integer.MAX_VALUE, 1000))
                .connectionTimeoutMs(connTimeout)
                .sessionTimeoutMs(sessionTimeout);

        client = builder.build();
        client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
            public void stateChanged(CuratorFramework client, ConnectionState state) {
                if (state == ConnectionState.LOST) {
                    CuratorZookeeperClient.this.stateChanged(StateListener.DISCONNECTED);
                } else if (state == ConnectionState.CONNECTED) {
                    CuratorZookeeperClient.this.stateChanged(StateListener.CONNECTED);
                } else if (state == ConnectionState.RECONNECTED) {
                    CuratorZookeeperClient.this.stateChanged(StateListener.RECONNECTED);
                }
            }
        });
        client.start();
    }

    public void createPersistent(String path) {
        try {
            client.create().forPath(path);
        } catch (NodeExistsException e) {
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public void createEphemeral(String path) {
        try {
            client.create().withMode(CreateMode.EPHEMERAL).forPath(path);
        } catch (NodeExistsException e) {
            logger.warn("path [{}] already exist, delete it", path);
            delete(path);
            try {
                client.create().withMode(CreateMode.EPHEMERAL).forPath(path);
            } catch (Exception e1) {
                throw new IllegalStateException(e1.getMessage(), e1);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public void delete(String path) {
        try {
            client.delete().forPath(path);
        } catch (NoNodeException e) {
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public List<String> getChildren(String path) {
        try {
            return client.getChildren().forPath(path);
        } catch (NoNodeException e) {
            return null;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public boolean isConnected() {
        return client.getZookeeperClient().isConnected();
    }

    public void doClose() {
        client.close();
    }

    private class CuratorWatcherImpl implements CuratorWatcher {

        private volatile ChildListener listener;

        public CuratorWatcherImpl(ChildListener listener) {
            this.listener = listener;
        }

        public void unwatch() {
            this.listener = null;
        }

        public void process(WatchedEvent event) throws Exception {
            if (listener != null && event.getPath() != null) {
                listener.childChanged(event.getPath(), client.getChildren().usingWatcher(this).forPath(event.getPath()));
            }
        }
    }

    @Override
    protected CuratorWatcher createTargetChildListener(String path, ChildListener listener)
    {
        return new CuratorWatcherImpl(listener);
    }

    @Override
    protected List<String> addTargetChildListener(String path, CuratorWatcher listener) {
        try {
            return client.getChildren().usingWatcher(listener).forPath(path);
        } catch (NoNodeException e) {
            return null;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    protected void removeTargetChildListener(String path, CuratorWatcher listener) {
        ((CuratorWatcherImpl) listener).unwatch();
    }

    @Override
    public Object readData(String path, boolean ifReturnNull)
    {
        try {
            return client.getData().forPath(path);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public void writeData(String path, Object data)
    {
        throw new IllegalStateException("unimplement method");
    }
}
