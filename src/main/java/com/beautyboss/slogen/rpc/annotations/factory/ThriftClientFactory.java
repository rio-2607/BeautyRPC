package com.beautyboss.slogen.rpc.annotations.factory;

import com.beautyboss.slogen.rpc.client.thrift.ThriftClient;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.ClassUtils;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class ThriftClientFactory implements FactoryBean {

    private ThriftClient thriftClient;

    private Class<?> serviceClazz;

    private Class<?> ifaceClazz;

    public void setThriftClient(ThriftClient thriftClient) {
        this.thriftClient = thriftClient;
    }

    public void setServiceClazz(Class<?> serviceClazz) {
        this.serviceClazz = serviceClazz;
        try {
            this.ifaceClazz = ClassUtils.forName(serviceClazz.getName() + "$Client", this.getClass().getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getObject() throws Exception {
        return thriftClient.getClient(serviceClazz);
    }

    @Override
    public Class getObjectType() {
        return ifaceClazz;
    }

    @Override
    public boolean isSingleton() {
        // TODO Auto-generated method stub
        return false;
    }
}
