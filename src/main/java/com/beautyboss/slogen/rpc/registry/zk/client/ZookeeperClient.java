package com.beautyboss.slogen.rpc.registry.zk.client;

import java.util.List;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public interface ZookeeperClient {
    String ZK_CURATOR_CLIENT = "curator";
    String ZK_ZKCLIENT_CLIENT = "zkclient";

    void create(String path, boolean ephemeral);

    void delete(String path);

    List<String> getChildren(String path);

    Object readData(String path, boolean ifReturnNull);

    void writeData(String path, Object data);

    List<String> addChildListener(String path, ChildListener listener);

    void removeChildListener(String path, ChildListener listener);

    void addStateListener(StateListener listener);

    void removeStateListener(StateListener listener);

    boolean isConnected();

    void close();

    String getHosts();

}
