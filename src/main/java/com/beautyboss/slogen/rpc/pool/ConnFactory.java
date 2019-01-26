package com.beautyboss.slogen.rpc.pool;

import com.beautyboss.slogen.rpc.Server;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public abstract class ConnFactory<T> {

    public abstract T makeObject(Server server) throws Exception;

    public abstract void destroyObject(T t) throws Exception;

    public abstract boolean validateObject(T t);

    public abstract void activateObject(T obj) throws Exception;

    public abstract void passivateObject(T obj) throws Exception;
}
