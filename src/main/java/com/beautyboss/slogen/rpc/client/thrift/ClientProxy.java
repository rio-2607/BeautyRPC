package com.beautyboss.slogen.rpc.client.thrift;

import com.beautyboss.slogen.rpc.service.Service;
import com.beautyboss.slogen.rpc.exceptions.RpcException;
import com.beautyboss.slogen.rpc.lb.LoadBalance;
import com.beautyboss.slogen.rpc.lb.impl.RRLoadBalance;
import com.beautyboss.slogen.rpc.pool.ConnWrapObject;
import com.beautyboss.slogen.rpc.registry.NotifyListener;
import com.beautyboss.slogen.rpc.registry.Registry;
import com.beautyboss.slogen.rpc.registry.sstatic.StaticRegistry;
import com.beautyboss.slogen.rpc.util.JsonUtil;
import com.beautyboss.slogen.rpc.util.NetUtil;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.TServiceClientFactory;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.TTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class ClientProxy implements MethodInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(ClientProxy.class);
    private static final Logger thriftClientStatlogger = LoggerFactory.getLogger("thriftClientStatlogger");

    /* init by framework */
    private AtomicBoolean inited = new AtomicBoolean();
    private Service service;
    private AtomicReference<List<Service>> servicesRef = new AtomicReference<>();
    private NotifyListener listener;
    private ThriftTSocketPool pool;
    private String ifaceName;
    private Class<?> objectClass;
    private LoadBalance loadbalance;
    private Registry staticRegistry;
    private ThriftClientConfig config;
    private TServiceClientFactory<TServiceClient> clientFactory;

    /* init by user */
    private boolean isMulti;
    private Class<?> ifaceClass;
    private boolean register;
    private String upstreamAddrs;
    private TTransportFactory transportFactory;
    private TProtocolFactory protocolFactory;
    private String lbType = LoadBalance.LOADBALANCE_RR;
    private ClassLoader classLoader;
    private int retries = -1;
    private int socketTimeout = -1;
    private boolean noKeepalive = false;

    /**
     *
     * @param ifaceClass 接口类
     */
    public ClientProxy(Class<?> ifaceClass) {
        this(ifaceClass, true);
    }

    /**
     *
     * @param ifaceClass 接口类
     * @param isMulti 服务端是否支持multi processor
     */
    public ClientProxy(Class<?> ifaceClass, boolean isMulti) {
        this.isMulti = isMulti;
        this.ifaceClass = ifaceClass;
        this.ifaceName = ifaceClass.getName();
    }

    /**
     * 初始化客户端
     */
    void init(ThriftClientConfig config) {
        if (ifaceClass == null) {
            throw new IllegalArgumentException("ifaceClass == null");
        }

        if (!inited.compareAndSet(false, true)) {
            return;
        }

        logger.info("init " + ifaceName + " client proxy");

        this.config = config;

        /* 如果没有配置了注册中心，则配置静态服务地址 */
        if (!register) {
            if (upstreamAddrs == null) {
                throw new IllegalArgumentException("register is false but no upstream addrs config");
            } else {
                staticRegistry = new StaticRegistry(upstreamAddrs, config.heartbeatPeriod);
            }
        }

        if (LoadBalance.LOADBALANCE_RR.equals(lbType)) {
            loadbalance = new RRLoadBalance();
        } else {
            throw new IllegalStateException("unspport loadbalance type");
        }

        if (classLoader == null) {
            classLoader = ClientProxy.class.getClassLoader();
        }

        try {
            objectClass = classLoader.loadClass(ifaceName + "$Client");
            @SuppressWarnings("unchecked")
            Class<TServiceClientFactory<TServiceClient>> fi =
                    (Class<TServiceClientFactory<TServiceClient>>)classLoader.loadClass(ifaceName + "$Client$Factory");
            clientFactory = fi.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            if (staticRegistry != null) {
                staticRegistry.destroy();
            }

            throw new RpcException("init client failure", e);
        }

        /* default is binary protocol */
        if (protocolFactory == null) {
            protocolFactory = new TBinaryProtocol.Factory();
        }

        if (transportFactory == null) {
            transportFactory = new TTransportFactory();
        }

        service = new Service(NetUtil.getLocalHost(), config.port, ifaceName, Service.CONSUMER_CATEGORY);

        listener = new NotifyListener() {
            @Override
            public void notify(List<Service> news)
            {
                List<Service> olds = servicesRef.get();

                logger.info("receive service update notify, old:{}, new:{}", JsonUtil.toJson(olds), JsonUtil.toJson(news));
                if (olds != null) {
                    for (int i = 0; i < olds.size(); i++) {
                        Service old = olds.get(i);
                        //保险起见干掉所有的连接
                        logger.info("clear pool start {}", System.currentTimeMillis());
                        pool.clear(old.getKey());
                        logger.info("clear pool end {}", System.currentTimeMillis());
                    }
                }

                servicesRef.set(news);
            }
        };

        if (staticRegistry != null) {
            staticRegistry.subscribe(ifaceName, listener);
        }

        pool = new ThriftTSocketPool(config.poolConfig);
    }

    /**
     * 关闭客户端
     */
    void stop() {
        if (staticRegistry != null) {
            staticRegistry.unsubscribe(ifaceName, listener);
            staticRegistry.destroy();
            staticRegistry = null;
        }

        pool.close();
    }

    /**
     * 获取客户端实例
     */
    TServiceClient getClient() {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(objectClass);
        enhancer.setCallback(this);

        Class<?>[] classArr = new Class<?>[1];
        classArr[0] = TProtocol.class;
        Object[] objArr = new Object[1];
        /* TserviceClient have no no-arg construtors, so we new a invalid object let it happy */
        objArr[0] = protocolFactory.getProtocol(null);
        return (TServiceClient) enhancer.create(classArr, objArr);
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            Constructor<?> constructor = objectClass.getConstructor(TProtocol.class); //获取构造方法
            Object myClassReflect = constructor.newInstance(new TBinaryProtocol(null)); //创建对象
            Class[] ctype = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                ctype[i] = args[i].getClass();
            }
            Method method1 = objectClass.getMethod(method.getName(), ctype);  //获取方法
            return method1.invoke(myClassReflect, args); //调用方法
        }

        List<Service> services = servicesRef.get();
        if (services == null || services.size() == 0) {
            throw new RpcException("no live service found");
        }

        int timeout = 0;
        int retries = this.retries;
        if (retries == -1) {
            retries = config.retries;
        }
        int socketTimeout = this.socketTimeout;
        if (socketTimeout == -1) {
            socketTimeout = config.socketTimeout;
        }
        for (int i = 0; i < retries + 1; i++) {
            Service service = loadbalance.select(services);
            String key = service.getKey();

            long start = System.currentTimeMillis();
            ConnWrapObject<TSocket> t = null;
            TSocket socket = null;
            int result = 0;
            boolean broken = false;
            boolean clear = false;
            try {
                if (this.noKeepalive ) {
                    socket = new TSocket(service.host, service.port);
                    socket.open();
                } else {
                    t = pool.getResource(key);
                    socket = t.getObject();
                }

                socket.setTimeout(socketTimeout);
                TTransport transport = transportFactory.getTransport(socket);

                TProtocol protocol;
                if (isMulti) {
                    protocol = new TMultiplexedProtocol(protocolFactory.getProtocol(transport), ifaceName);
                } else {
                    protocol = protocolFactory.getProtocol(transport);
                }
                return proxy.invoke(clientFactory.getClient(protocol), args);
            } catch (TTransportException e) {
                logger.error(" invoke " + service.toString() + " [" + method.getName() + "] failure: "
                        + ExceptionMsg.TTRAS[e.getType()] + "," + e.getMessage(), e);

                result = 1;
                broken = true;
                Throwable c = e.getCause();
                if (c != null && (c instanceof ConnectException || "Broken pipe".equals(c.getMessage()))) {
                    //if (c instanceof SocketException) { 是否有必要重试所有的套接字异常
                    clear = true;
                } else {
                    throw e;
                }

                /* connection exception, retry */
            } catch (TApplicationException e) {
                //thrift 返回空值会抛异常
                if (e.getType() == TApplicationException.MISSING_RESULT) {
                    return null;
                } else {
                    result = 1;
                    logger.error("invoke " + service.toString() + " ["  + method.getName() + "] got exception: "
                            + ExceptionMsg.TAPP[e.getType()] + "," + e.getMessage(), e);
                    throw e;
                }
            } catch (TProtocolException e) {
                result = 1;
                logger.error("invoke " + service.toString() + " [" + method.getName() + "] failure: "
                        + ExceptionMsg.TPRO[e.getType()] + "," + e.getMessage(), e);
                throw e;
            } catch (TException e) {
                result = 1;
                logger.error("invoke " + service.toString() + " ["  + method.getName() + "] failure: "
                        + e.getMessage(), e);
                throw e;
            } catch (Throwable e) {
                broken = true;
                result = 1;
                logger.error("invoke " + service.toString() + " ["  + method.getName() + "] failure,unknown exception: "
                        + e.getMessage(), e);
                throw e;
            } finally {
                long timeSpend = System.currentTimeMillis() - start;
                thriftClientStatlogger.info("{},{}-{},{},{}", key, service.name, method.getName(), timeSpend, result);

                if (this.noKeepalive) {
                    socket.close();
                } else {
                    if (t != null) {
                        if (clear) {
                            pool.clear(key);
                        } else {
                            if (!broken) {
                                pool.returnResource(key, t);
                            } else {
                                pool.returnBrokenResource(key, t);
                            }
                        }
                    }
                }
            }
        }

        if (timeout == retries + 1) {
            throw new RpcException("invoke " + method.getName() + " failed, timedout");
        } else {
            throw new RpcException("invoke " + method.getName() + " failed");
        }
    }

    public Class<?> getIfaceClass()
    {
        return ifaceClass;
    }

    public boolean isMulti()
    {
        return isMulti;
    }

    /**
     *
     * @param isMulti 设置服务端是否是TMulitProcessor
     */
    public ClientProxy setMulti(boolean isMulti)
    {
        this.isMulti = isMulti;
        return this;
    }

    /**
     * 设置接口类
     * @param ifaceClass
     */
    public ClientProxy setIfaceClass(Class<?> ifaceClass)
    {
        this.ifaceClass = ifaceClass;
        this.ifaceName = ifaceClass.getName();
        return this;
    }

    public TTransportFactory getTransportFactory()
    {
        return transportFactory;
    }

    /**
     * 设置thrift transport工厂，默认是TTransportFactory，如果服务是非阻塞模型
     * 则需要设置为TFramedTransport
     * @param transportFactory   transport工厂
     */
    public ClientProxy setTransportFactory(TTransportFactory transportFactory)
    {
        this.transportFactory = transportFactory;
        return this;
    }

    public TProtocolFactory getProtocolFactory()
    {
        return protocolFactory;
    }

    /**
     * 设置thrift 协议工厂，默认是TBinaryProtocol，要和服务端保持一致
     * @param protocolFactory   协议工厂
     */
    public ClientProxy setProtocolFactory(TProtocolFactory protocolFactory)
    {
        this.protocolFactory = protocolFactory;
        return this;
    }

    public String getUpstreamAddrs()
    {
        return upstreamAddrs;
    }

    /**
     * 设置服务地址，多个地址逗号分隔，如果不需要注册中心则需要这个配置
     * @param upstreamAddrs   服务地址
     */
    public ClientProxy setUpstreamAddrs(String upstreamAddrs)
    {
        this.upstreamAddrs = upstreamAddrs;
        return this;
    }

    public String getLbType()
    {
        return lbType;
    }

    /**
     * 设置负载均衡类型，目前支持roundrobin，默认为roundrobin
     * @param lbType   roundrobin
     */
    public ClientProxy setLbType(String lbType)
    {
        this.lbType = lbType;
        return this;
    }

    public boolean isRegister()
    {
        return register;
    }

    /**
     * 设置是否需要注册中心，默认不需要
     * @param register   默认false
     */
    public ClientProxy setRegister(boolean register)
    {
        this.register = register;
        return this;
    }

    public int getRetries()
    {
        return retries;
    }

    public ClientProxy setRetries(int retries)
    {
        this.retries = retries;
        return this;
    }

    public int getSocketTimeout()
    {
        return socketTimeout;
    }

    public ClientProxy setSocketTimeout(int socketTimeout)
    {
        this.socketTimeout = socketTimeout;
        return this;
    }

    public ClassLoader getClassLoader()
    {
        return classLoader;
    }

    public ClientProxy setClassLoader(ClassLoader classLoader)
    {
        this.classLoader = classLoader;
        return this;
    }

    public Service getService()
    {
        return service;
    }

    public NotifyListener getListener()
    {
        return listener;
    }

    public String getIfaceName()
    {
        return ifaceName;
    }

    public boolean isNoKeepalive()
    {
        return noKeepalive;
    }

    public ClientProxy setNoKeepalive(boolean noKeepalive)
    {
        this.noKeepalive = noKeepalive;
        return this;
    }
}
