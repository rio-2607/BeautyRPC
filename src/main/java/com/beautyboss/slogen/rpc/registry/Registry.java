package com.beautyboss.slogen.rpc.registry;

import com.beautyboss.slogen.rpc.Service;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public interface Registry {
    String getHosts();

    boolean isAvailable();

    void destroy();

    void register(Service service);

    void unregister(Service service);

    void subscribe(String serviceName, NotifyListener listener);

    void unsubscribe(String serviceName, NotifyListener listener);
}
