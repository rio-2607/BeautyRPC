package com.beautyboss.slogen.rpc.registry.zk.client;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public interface StateListener {

    int DISCONNECTED = 0;

    int CONNECTED = 1;

    int RECONNECTED = 2;

    void stateChanged(int connected);

}
