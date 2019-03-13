package com.beautyboss.slogen.rpc.client.thrift;

import com.beautyboss.slogen.rpc.service.Server;
import com.beautyboss.slogen.rpc.exceptions.RpcException;
import com.beautyboss.slogen.rpc.pool.ConnFactory;
import org.apache.thrift.transport.TSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class ThriftTSocketFactory extends ConnFactory<TSocket> {
    private static final Logger logger = LoggerFactory.getLogger(ThriftTSocketFactory.class);

    public TSocket makeObject(Server server) throws Exception {
        logger.debug("make object: " + server);

        TSocket socket = new TSocket(server.getHost(), server.getPort());
        socket.setTimeout(1000);
        socket.open();

        return socket;
    }

    public void destroyObject(TSocket t) throws Exception {
        logger.debug("destroy object");
        if (t.isOpen()) {
            t.close();
        }
    }

    public boolean validateObject(TSocket t) {
        return t.isOpen();
    }

    public void activateObject(TSocket t) throws Exception {
        if (!t.isOpen()) {
            throw new RpcException("socket closed");
        }
    }

    public void passivateObject(TSocket t) throws Exception {
        if (!t.isOpen()) {
            throw new RpcException("socket closed");
        }
    }
}
