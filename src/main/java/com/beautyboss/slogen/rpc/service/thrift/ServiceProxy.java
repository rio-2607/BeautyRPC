package com.beautyboss.slogen.rpc.service.thrift;

import com.beautyboss.slogen.rpc.exceptions.ServiceException;
import com.beautyboss.slogen.rpc.service.Service;
import com.beautyboss.slogen.rpc.util.NetUtil;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.thrift.TProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class ServiceProxy implements MethodInterceptor {
    private static final Logger thriftServiceStatLogger = LoggerFactory.getLogger("thriftServiceStatlogger");
    private static final ThriftServiceConfig config = ThriftServiceConfig.getConfig();

    /* init by framework */
    private AtomicBoolean inited = new AtomicBoolean();
    private AtomicLong requestNum = new AtomicLong();
    private AtomicLong rspSpend = new AtomicLong();
    private Service service;
    private String ifaceName;
    private Constructor<TProcessor> pconstructor;

    /* init by user */
    private Class<?> ifaceClass; // 定义的接口
    private ClassLoader classLoader;
    private Object ifaceImpl; // 接口实现，实现了ifaceClass$Iface
    private boolean register; //默认不注册

    /**
     * 创建服务代理, 成员需要通过setter方法设置，并手动调用init方法
     */
    public ServiceProxy() {}

    /**
     * 创建服务代理并且注册服务，需要配置注册中心
     * @param ifaceClass   接口类
     * @param ifaceImpl   接口实现
     */
    public ServiceProxy(Class<?> ifaceClass, Object ifaceImpl)
    {
        this(ifaceClass, ifaceImpl, true);
    }

    /**
     * 创建服务代理
     * @param ifaceClass  接口名字，使用完整的类名
     * @param ifaceImpl   接口实现
     * @param register    是否注册
     */
    public ServiceProxy(Class<?> ifaceClass, Object ifaceImpl, boolean register)
    {
        this.ifaceClass = ifaceClass;
        this.ifaceName = ifaceClass.getName();
        this.ifaceImpl = ifaceImpl;
        this.register = register;
    }

    /**
     * 初始化服务容器并启动服务
     */
    void init()
    {
        if (!inited.compareAndSet(false, true)) {
            return;
        }

        if (ifaceName == null) {
            throw new IllegalArgumentException("config ifaceName == null");
        }

        if (ifaceImpl == null) {
            throw new IllegalArgumentException("config ifaceImpl == null");
        }

        String host;
        if (config.host == null) {
            host = NetUtil.getLocalHost();
        } else {
            host = config.host;
        }

        service = new Service(host, config.port, ifaceName, Service.PROVIDER_CATEGORY);

        if (classLoader == null) {
            classLoader = ServiceProxy.class.getClassLoader();
        }

        try {
            @SuppressWarnings("unchecked")
            Class<TProcessor> processorClass = (Class<TProcessor>) classLoader.loadClass(ifaceName + "$Processor");
            Class<?> ifaceClass = (Class<?>) classLoader.loadClass(ifaceName + "$Iface");
            pconstructor = processorClass.getConstructor(ifaceClass);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
            throw new ServiceException("init service proxy failure", e);
        }
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        requestNum.incrementAndGet();

        long start = System.currentTimeMillis();
        try {
            return proxy.invoke(ifaceImpl, args);
        } finally {
            long timeSpend = System.currentTimeMillis() - start;
            rspSpend.addAndGet(timeSpend);
            thriftServiceStatLogger.info("{},{}-{},{}", service.getKey(), service.name,
                    method.getName(), timeSpend);
        }
    }

    Object getProxy()
    {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(ifaceImpl.getClass());
        enhancer.setCallback(this);

        return enhancer.create();
    }

    public Class<?> getIfaceClass()
    {
        return ifaceClass;
    }

    /**
     * 设置接口名
     * @param ifaceClass   接口类
     */
    public void setIfaceClass(Class<?> ifaceClass)
    {
        this.ifaceClass = ifaceClass;
        this.ifaceName = ifaceClass.getName();
    }

    public Object getIfaceImpl()
    {
        return ifaceImpl;
    }

    /**
     * 设置接口实现
     * @param ifaceImpl
     */
    public void setIfaceImpl(Object ifaceImpl)
    {
        this.ifaceImpl = ifaceImpl;
    }

    public boolean isRegister()
    {
        return register;
    }

    /**
     * 设置是否注册服务到注册中心
     * @param register 默认false
     */
    public void setRegister(boolean register)
    {
        this.register = register;
    }

    public AtomicLong getRequestNum()
    {
        return requestNum;
    }

    public AtomicLong getRspSpend()
    {
        return rspSpend;
    }

    void setRspSpend(AtomicLong rspSpend)
    {
        this.rspSpend = rspSpend;
    }

    public Service getService()
    {
        return service;
    }

    public Constructor<TProcessor> getPconstructor()
    {
        return pconstructor;
    }

    public String getIfaceName()
    {
        return ifaceName;
    }

    public ClassLoader getClassLoader()
    {
        return classLoader;
    }

    public ServiceProxy setClassLoader(ClassLoader classLoader)
    {
        this.classLoader = classLoader;
        return this;
    }
}
