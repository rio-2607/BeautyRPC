package com.beautyboss.slogen.rpc.registry.zk.client.impl;

import com.beautyboss.slogen.rpc.registry.zk.client.AbstractZookeeperClient;
import com.beautyboss.slogen.rpc.registry.zk.client.ChildListener;
import com.beautyboss.slogen.rpc.registry.zk.client.StateListener;


import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNoNodeException;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.apache.zookeeper.Watcher.Event.KeeperState;


import java.util.List;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class ZkclientZookeeperClient extends AbstractZookeeperClient<IZkChildListener> {
    private final ZkClient client;

    private volatile KeeperState state = KeeperState.SyncConnected;

    public ZkclientZookeeperClient(String hosts, int sessionTimeout, int connTimeout) {
        super(hosts);
        client = new ZkClient(hosts, sessionTimeout, connTimeout);

        client.subscribeStateChanges(new IZkStateListener() {
            public void handleStateChanged(KeeperState state) throws Exception {
                ZkclientZookeeperClient.this.state = state;
                if (state == KeeperState.Disconnected) {
                    stateChanged(StateListener.DISCONNECTED);
                } else if (state == KeeperState.SyncConnected) {
                    stateChanged(StateListener.CONNECTED);
                }
            }

            public void handleNewSession() throws Exception {
                stateChanged(StateListener.RECONNECTED);
            }
        });
    }

    @Override
    public Object readData(String path, boolean ifReturnNull) {
        return client.readData(path, ifReturnNull);
    }

    @Override
    public void writeData(String path, Object data) {
        client.writeData(path, data);
    }

    public void createPersistent(String path) {
        try {
            client.createPersistent(path, true);
        } catch (ZkNodeExistsException e) {
        }
    }

    public void createEphemeral(String path) {
        try {
            client.createEphemeral(path);
        } catch (ZkNodeExistsException e) {
            logger.warn("path [{}] already exist, delete it", path);
            delete(path);
            client.createEphemeral(path);
        }
    }

    public void delete(String path) {
        try {
            client.delete(path);
        } catch (ZkNoNodeException e) {
        }
    }

    public List<String> getChildren(String path) {
        try {
            return client.getChildren(path);
        } catch (ZkNoNodeException e) {
            return null;
        }
    }

    public boolean isConnected() {
        return state == KeeperState.SyncConnected;
    }

    public void doClose() {
        client.close();
    }

    public IZkChildListener createTargetChildListener(String path, final ChildListener listener) {
        return new IZkChildListener() {
            public void handleChildChange(String parentPath, List<String> currentChilds)
                    throws Exception {
                listener.childChanged(parentPath, currentChilds);
            }
        };
    }

    public List<String> addTargetChildListener(String path, final IZkChildListener listener) {
        return client.subscribeChildChanges(path, listener);
    }

    public void removeTargetChildListener(String path, IZkChildListener listener) {
        client.unsubscribeChildChanges(path, listener);
    }
}
