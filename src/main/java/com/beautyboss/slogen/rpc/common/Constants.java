package com.beautyboss.slogen.rpc.common;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class Constants {

    public static final String DEFAULT_CONFIG_PROPERTIES = "thrift-rpc.properties";
    public static final String CONFIG_PROPERTIES = "application.properties";
    public static final String SPRINGBOOT_ENV = "spring.profiles.active";
    public static final String SPRINGBOOT_ENV_PROPERTIES = "application-%s.properties";


    /* thrift client default config */
    public static final int DEFAULT_RETRIES = 2;
    public static final int DEFAULT_CLIENT_TIMEOUT = 5000;
    public static final boolean DEFAULT_NOKEEPALIVE = false;
    public static final boolean DEFAULT_ISMULTI_PROTOCOL = true;

    /* static registry default config */
    public static final int DEFAULT_HEARTBEAT_PERIOD = 5000;

    /* thrift connection pool default config */
    public static final int DEFAULT_MAX_IDLE = 8;
    public static final boolean DEFAULT_TEST_ON_CREATE = false;
    public static final boolean DEFAULT_TEST_ON_BORROW = false;
    public static final boolean DEFAULT_TEST_ON_RETURN = false;

    /* client config properties */
    public static final String CONFIG_CLIENT_RETRIES = "thrift.client.retries";
    public static final String CONFIG_CLIENT_TIMEOUT = "thrift.client.timeout";
    public static final String CONFIG_HEARTBEAT_PERIOD = "thrift.client.heartbeat.period";

    public static final String CONFIG_CLIENT_POOL_MAXIDLE = "thrift.connpool.maxidle";
    public static final String CONFIG_CLIENT_POOL_TESTONCREATE = "thrift.connpool.testoncreate";
    public static final String CONFIG_CLIENT_POOL_TESTONBORROW = "thrift.connpool.testonborrow";
    public static final String CONFIG_CLIENT_POOL_TESTONRETURN = "thrift.connpool.testonreturn";


    /* thrift service config */
    public static final int DEFAULT_PORT = 9090;

    /* thread pool default config */
    public static final int DEFAULT_REQUEST_TIMEOUT = 5000;
    public static final int DEFAULT_MIN_WORKER_THREADS = 1;
    public static final int DEFAULT_MAX_WORKER_THREADS = 5;

    /* tframe default max length */
    public static final int DEFAULT_TFRAME_MAX_LENGTH = 16384000;

    /* thread selector default config */
    public static final int DEFAULT_SELECTOR_THREADS = 2;
    public static final int DEFAULT_WORKER_THREADS = 5;
    public static final int DEFAULT_QUEUE_SIZE_PER_THREADS = 20;

    /* failback registry default config */
    public static final int DEFAULT_RETRY_PERIOD = 5000;
    public static final boolean DEFAULT_CHECK_WHEN_STARTUP = false;

    /* zookeeper registry default config */
    public static final int DEFAULT_REGISTRY_DELAY = 0;

    public static final String DEFAULT_GROUP = "thrift";
    public static final String PATH_SEPARATOR = "/";
    public static final boolean DEFAULT_EPHEMERAL_NODE = true;
    public static final int DEFAULT_TIMEOUT = 2000;
    public static final int DEFAULT_SESSION_TIMEOUT = 5000;

    public static final String CONFIG_SERVICE_HOST = "thrift.host";
    public static final String CONFIG_SERVICE_PORT = "thrift.port";

    public static final String CONFIG_SERVICE_REQUEST_TIMEOUT = "thrift.threadpool.reqtimeout";
    public static final String CONFIG_MIN_WORKER_THREADS = "thrift.threadpool.minworkerthreads";
    public static final String CONFIG_MAX_WORKER_THREADS = "thrift.threadpool.maxworkerthreads";

    public static final String CONFIG_TFRAME_MAX_LENGTH = "thrift.tframe.maxlength";
    public static final String CONFIG_SELECTOR_THREADS = "thrift.threadselector.selectorthreads";
    public static final String CONFIG_WORKER_THREADS = "thrift.threadselector.workerthreads";
    public static final String CONFIG_QUEUE_SIZE_PER_THREADS = "thrift.threadselector.queuesize";


    public static final String CONFIG_REGISTRY_ZKCLIENT = "thrift.registry.zkclient";
    public static final String CONFIG_REGISTRY_DELAY = "thrift.registry.delay";
    public static final String CONFIG_REGISTRY_ADDRS = "thrift.registry.addr";
    public static final String CONFIG_REGISTRY_SESSION_TIMEOUT = "thrift.registry.session";
    public static final String CONFIG_REGISTRY_RETRY_PERIOD = "thrift.registry.retry.period";
    public static final String CONFIG_REGISTRY_TIMEOUT = "thrift.registry.timeout";
    public static final String CONFIG_REGISTRY_CHECK_WHEN_STARTUP = "thrift.registry.check";
}
