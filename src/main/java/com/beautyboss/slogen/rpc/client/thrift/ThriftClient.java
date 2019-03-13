package com.beautyboss.slogen.rpc.client.thrift;

import com.beautyboss.slogen.rpc.common.Constants;
import com.beautyboss.slogen.rpc.exceptions.RpcException;
import com.beautyboss.slogen.rpc.registry.Registry;
import com.beautyboss.slogen.rpc.registry.support.RegistryContainer;
import org.apache.thrift.TServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class ThriftClient {
    private static final Logger logger = LoggerFactory.getLogger(ThriftClient.class);

    private Map<String, ClientProxy> unmodifyProxys = new HashMap<>();
    private Map<String, TServiceClient> unmodifyClients = new HashMap<>();

    /* init by framework */
    private AtomicBoolean inited = new AtomicBoolean();
    private AtomicBoolean stoped = new AtomicBoolean();
    private Registry registry;
    private ThriftClientConfig config;

    /*
     * init by user
     */
    private List<ClientProxy> proxyList = new ArrayList<>();

    /**
     * 使用默认的配置文件初始化
     */
    public ThriftClient() {
        this(Constants.DEFAULT_CONFIG_PROPERTIES);
    }

    /**
     * @param configFile 指定配置文件
     */
    public ThriftClient(String configFile) {
        config = new ThriftClientConfig(configFile);
    }

    public ThriftClient(InputStream inputStream) {
        config = new ThriftClientConfig(inputStream);
    }

    /**
     * @return 新创建一个实例
     */
    public static ThriftClient getInstance() {
        return new ThriftClient();
    }

    /**
     * 增加一个代理到客户端池
     *
     * @param proxy
     */
    public void add(ClientProxy proxy) {
        proxyList.add(proxy);
    }

    /**
     * 初始化客户端
     */
    public void init() {
        if (!inited.compareAndSet(false, true)) {
            return;
        }

        for (int i = 0; i < proxyList.size(); i++) {
            unmodifyProxys.put(proxyList.get(i).getIfaceName(), proxyList.get(i));
        }

        boolean register = false;
        for (ClientProxy client : unmodifyProxys.values()) {
            client.init(config);
            unmodifyClients.put(client.getIfaceName(), client.getClient());
            if (client.isRegister()) {
                register = true;
            }
        }

        /* 如果配置了注册中心，则初始化注册中心 */
        if (register) {
            registry = RegistryContainer.getRegistry(config.regcfg);
            if (registry == null) {
                throw new IllegalStateException("no register config found");
            }
        }

        try {
            if (registry != null) {
                for (ClientProxy client : unmodifyProxys.values()) {
                    if (client.isRegister()) {
                        registry.register(client.getService());
                        registry.subscribe(client.getIfaceName(), client.getListener());
                    }
                }
            }
        } catch (Exception e) {
            stop();
            throw new RpcException("init client failure", e);
        }
    }

    /**
     * 关闭客户端
     */
    public void stop() {
        if (!inited.get()) {
            logger.warn("client not start, so stop do nothing");
            return;
        }

        if (!stoped.compareAndSet(false, true)) {
            return;
        }

        for (ClientProxy client : unmodifyProxys.values()) {
            client.stop();
            if (registry != null && client.isRegister()) {
                registry.unregister(client.getService());
                registry.unsubscribe(client.getIfaceName(), client.getListener());
            }
        }
    }

    /**
     * 获取客户端实例
     */
    public TServiceClient getClient(Class<?> ifaceClass) {
        if (!inited.get()) {
            throw new IllegalStateException("ThriftClient not inited");
        }

        return unmodifyClients.get(ifaceClass.getName());
    }

    public List<ClientProxy> getProxyList() {
        return proxyList;
    }

    public void setProxyList(List<ClientProxy> proxyList) {
        this.proxyList = proxyList;
    }
}

