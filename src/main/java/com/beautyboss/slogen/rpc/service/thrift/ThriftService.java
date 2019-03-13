package com.beautyboss.slogen.rpc.service.thrift;

import com.beautyboss.slogen.rpc.exceptions.ServiceException;
import com.beautyboss.slogen.rpc.registry.Registry;
import com.beautyboss.slogen.rpc.registry.support.RegistryContainer;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.*;
import org.apache.thrift.transport.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class ThriftService {
    private static final Logger logger = LoggerFactory.getLogger(ThriftService.class);
    private static final ThriftServiceConfig config = ThriftServiceConfig.getConfig();

    public static final String THRIFT_THREAD_POOL = "threadpool";
    public static final String THRIFT_THREAD_THREADSELECTOR = "threadselector";

    /* init by framework */
    private AtomicBoolean inited = new AtomicBoolean();
    private AtomicBoolean stoped = new AtomicBoolean();
    private Registry registry;
    TServer server;

    /* init by user */
    private TTransportFactory transportFactory;
    private TProtocolFactory protocolFactory;
    private String serverType = THRIFT_THREAD_THREADSELECTOR;
    private boolean isMulti = true;
    private List<ServiceProxy> services;

    /**
     * 创建服务容器, 成员需要通过setter方法设置，并手动调用init方法
     */
    public ThriftService() {
        this(new TTransportFactory(), new TBinaryProtocol.Factory(), THRIFT_THREAD_THREADSELECTOR);
    }

    /**
     * 创建服务容器，使用指定的thrift服务模型(threadpool或threadselector)
     * 当服务模型是threadselector时，transport工厂为TFramedTransport.Factory，否则为TTransportFacotry
     * 使用默认的协议工厂TBinaryProtocol.Factory
     *
     * @param serverType 服务模型
     */
    public ThriftService(String serverType) {
        this(new TTransportFactory(), new TBinaryProtocol.Factory(), serverType);
    }

    /**
     * 创建服务容器，使用指定的transport工厂和服务模型(threadpool或threadselector)
     *
     * @param transportFactory 默认为TTransportFactory, 当服务类型为threadselector时这个参数不起作用
     * @param serverType       服务类型，默认为threadselector
     */
    public ThriftService(TTransportFactory transportFactory, String serverType) {
        this(transportFactory, new TBinaryProtocol.Factory(), serverType);
    }

    /**
     * 创建服务容器，使用指定的协议工厂和服务模型(threadpool或threadselector)
     * 当服务模型是threadselector时，transport工厂为TFramedTransport.Factory，否则为TTransportFacotry
     *
     * @param protocolFactory 默认为TBinaryProtocol.Factory
     * @param serverType      服务类型，默认为threadselector
     */
    public ThriftService(TProtocolFactory protocolFactory, String serverType) {
        this(new TTransportFactory(), protocolFactory, serverType);
    }

    /**
     * 创建服务容器，使用指定的协议工厂、tranport工厂和thrift服务模型(threadpool或threadselector)
     *
     * @param transportFactory 默认为TTransportFactory, 当服务类型为threadselector时忽略这个参数
     * @param protocolFactory  默认为TBinaryProtocol.Factory
     * @param serverType       服务类型，默认为threadselector
     */
    public ThriftService(TTransportFactory transportFactory, TProtocolFactory protocolFactory, String serverType) {
        this.transportFactory = transportFactory;
        this.protocolFactory = protocolFactory;
        this.serverType = serverType;
        this.services = new ArrayList<>();
    }

    /**
     * 添加服务代理到服务容器
     *
     * @param service 服务代理
     */
    public void add(ServiceProxy service) {
        services.add(service);
    }

    /**
     * 初始化服务容器并启动服务
     *
     * @throws ServiceException if init failure
     */
    public void init() {
        if (!inited.compareAndSet(false, true)) {
            return;
        }

        if (!isMulti && services.size() > 1) {
            throw new IllegalStateException("not multi processor and services num > 0");
        }

        boolean register = false;
        for (int i = 0; i < services.size(); i++) {
            services.get(i).init();
            register = register || services.get(i).isRegister();
        }

        if (register) {
            registry = RegistryContainer.getRegistry(config.regcfg);
            if (registry == null) {
                throw new IllegalStateException("no registry config found");
            }
        }

        logger.info("init server use " + serverType + " service model");

        /* default is binary protocol */
        if (protocolFactory == null) {
            logger.info("user default binary protocol");
            protocolFactory = new TBinaryProtocol.Factory();
        }

        if (transportFactory == null) {
            logger.info("user default transport factory");
            transportFactory = new TTransportFactory();
        }

        try {
            if (serverType.equals(THRIFT_THREAD_THREADSELECTOR)) {
                server = initTThreadedSelectorServer();
            } else if (serverType.equals(THRIFT_THREAD_POOL)) {
                server = initThreadPoolServer();
            } else {
                throw new ServiceException("unsupported server type");
            }
        } catch (Exception e) {
            if (registry != null) {
                registry.destroy();
            }
            throw new ServiceException("init failure", e);
        }

        if (registry != null) {
            server.setServerEventHandler(new AbstractTServerEventHandler() {
                @Override
                public void preServe() {
                    if (config.rigistryDelay > 0) {
                        try {
                            Thread.sleep(config.rigistryDelay);
                        } catch (Exception e) {
                            logger.error("sleep intercept", e);
                        }
                    }

                    for (int i = 0; i < services.size(); i++) {
                        if (services.get(i).isRegister()) {
                            registry.register(services.get(i).getService());
                        }
                    }
                }
            });

            //清理zk资源
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    RegistryContainer.destroyAll();
                }
            });
        }

        try {
            server.serve();
        } finally {
            stop();
        }
    }

    public void stop() {
        if (!inited.get()) {
            logger.warn("service not start, so stop do nothing");
            return;
        }

        if (!stoped.compareAndSet(false, true)) {
            return;
        }

        if (registry != null) {
            for (int i = 0; i < services.size(); i++) {
                registry.unregister(services.get(i).getService());
            }
            registry = null;
        }

        RegistryContainer.destroyAll();

        if (server.isServing()) {
            server.stop();
        }
    }

    private TServer initTThreadedSelectorServer() throws Exception {
        TNonblockingServerSocket serverSocket = new TNonblockingServerSocket(config.port);
        TThreadedSelectorServer.Args args = new TThreadedSelectorServer.Args(serverSocket);

        if (isMulti) {
            TMultiplexedProcessor processor = new TMultiplexedProcessor();
            for (int i = 0; i < services.size(); i++) {
                ServiceProxy service = services.get(i);
                /* 服务端是否有必要用cglib代理? */
                //processor.registerProcessor(service.getIfaceName(),
                //service.getPconstructor().newInstance(service.getProxy()));

                processor.registerProcessor(service.getIfaceName(),
                        service.getPconstructor().newInstance(service.getIfaceImpl()));

            }
            args.processor(processor);
        } else {
            ServiceProxy service = services.get(0);
            //args.processor(service.getPconstructor().newInstance(service.getProxy()));
            args.processor(service.getPconstructor().newInstance(service.getIfaceImpl()));
        }

        args.protocolFactory(protocolFactory);
        args.transportFactory(new TFramedTransport.Factory(config.tframeMaxLength));
        args.selectorThreads(config.selectorThreads);
        args.workerThreads(config.workerThreads);
        args.acceptQueueSizePerThread(config.acceptQueueSizePerThread);

        return new TThreadedSelectorServer(args);
    }

    private TServer initThreadPoolServer() throws Exception {
        TThreadPoolServer.Args args = new TThreadPoolServer.Args(new TServerSocket(config.port));

        if (isMulti) {
            TMultiplexedProcessor processor = new TMultiplexedProcessor();
            for (int i = 0; i < services.size(); i++) {
                ServiceProxy service = services.get(i);
                /* 服务端是否有必要用cglib代理? */
                //processor.registerProcessor(service.getIfaceName(),
                //        service.getPconstructor().newInstance(service.getProxy()));

                processor.registerProcessor(service.getIfaceName(),
                        service.getPconstructor().newInstance(service.getIfaceImpl()));

            }
            args.processor(processor);
        } else {
            ServiceProxy service = services.get(0);
            //args.processor(service.getPconstructor().newInstance(service.getProxy()));
            args.processor(service.getPconstructor().newInstance(service.getIfaceImpl()));
        }

        args.protocolFactory(protocolFactory);
        args.transportFactory(transportFactory);
        args.minWorkerThreads(config.minWorkerThreads);
        args.maxWorkerThreads(config.maxWorkerThreads);

        return new TThreadPoolServer(args);
    }

    private abstract class AbstractTServerEventHandler implements TServerEventHandler {

        @Override
        public ServerContext createContext(TProtocol arg0, TProtocol arg1) {
            return null;
        }

        @Override
        public void deleteContext(ServerContext arg0, TProtocol arg1, TProtocol arg2) {
        }

        @Override
        public abstract void preServe();

        @Override
        public void processContext(ServerContext arg0, TTransport arg1,
                                   TTransport arg2) {
        }
    }

    public TTransportFactory getTransportFactory() {
        return transportFactory;
    }

    public void setTransportFactory(TTransportFactory transportFactory) {
        this.transportFactory = transportFactory;
    }

    public TProtocolFactory getProtocolFactory() {
        return protocolFactory;
    }

    public void setProtocolFactory(TProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    public String getServerType() {
        return serverType;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    public boolean isMulti() {
        return isMulti;
    }

    public void setMulti(boolean isMulti) {
        this.isMulti = isMulti;
    }

    public List<ServiceProxy> getServices() {
        return services;
    }

    public void setServices(List<ServiceProxy> services) {
        this.services = services;
    }

}
