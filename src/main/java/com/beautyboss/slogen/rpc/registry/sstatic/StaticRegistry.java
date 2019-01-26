package com.beautyboss.slogen.rpc.registry.sstatic;

import com.beautyboss.slogen.rpc.Server;
import com.beautyboss.slogen.rpc.Service;
import com.beautyboss.slogen.rpc.common.Constants;
import com.beautyboss.slogen.rpc.registry.NotifyListener;
import com.beautyboss.slogen.rpc.registry.support.AbstractRegistry;
import org.apache.commons.lang.StringUtils;
import org.apache.thrift.transport.TSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class StaticRegistry extends AbstractRegistry {
    private static final Logger logger = LoggerFactory.getLogger(StaticRegistry.class);

    // 定时任务执行器
//    private final ScheduledExecutorService checkExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("StaticRegistyCheckTimer", true));;

    /* init by framework */
    private ScheduledFuture<?> checkTimer;
    private List<Server> servers;

    /* init by user */
    private int heartbeatPeriod = Constants.DEFAULT_HEARTBEAT_PERIOD;

    public StaticRegistry() {
    }

    public StaticRegistry(String hosts, int heartbeatPeriod) {
        this.hosts = hosts;
        this.heartbeatPeriod = heartbeatPeriod;
        init();
    }

    public void init() {
        String addrs = getHosts();
        if (addrs == null || "".equals(addrs.trim())) {
            throw new IllegalStateException("static service address == null");
        }

        parseServer(addrs);
//
//        if (getHeartbeatPeriod() > 0) {
//            checkTimer = checkExecutor.scheduleWithFixedDelay(new Runnable() {
//                public void run() {
//                    try {
//                        check();
//                    } catch (Throwable t) { // 防御性容错
//                        logger.error("Unexpected error occur at failed retry, cause: " + t.getMessage(), t);
//                    }
//                }
//            }, getHeartbeatPeriod(), getHeartbeatPeriod(), TimeUnit.MILLISECONDS);
//        }
    }

    private void parseServer(String addr) {
        List<Server> srvList = new ArrayList<>();
        String[] srv = StringUtils.split(addr, ',');
        for (int i = 0; i < srv.length; i++) {
            String server = srv[i];
            String[] info = StringUtils.split(server, ':');
            srvList.add(new Server(info[0], Integer.parseInt(info[1])));
        }

        Random rand = new Random();
        rand.setSeed(System.currentTimeMillis());
        Collections.shuffle(srvList, rand);
        servers = new ArrayList<Server>(srvList);
    }

    public void subscribe(final String service, final NotifyListener listener) {
        super.subscribe(service, listener);

        List<Service> services = new ArrayList<>();
        for (int i = 0; i < servers.size(); i++) {
            Server server = servers.get(i);
            Service s = new Service(server.getHost(), server.getPort(), service, Service.PROVIDER_CATEGORY);

            services.add(s);
        }

        notify(service, listener, services);
    }

    private void check() {
        List<Server> newServers = new ArrayList<>();

        for (int i = 0; i < servers.size(); i++) {
            boolean failure = false;
            Server server = servers.get(i);
            TSocket socket = null;
            try {
                logger.debug("heartbeat for " + server);
                socket = new TSocket(server.getHost(), server.getPort());
                socket.setTimeout(2000);
                socket.open();
            } catch (Exception e) {
                failure = true;
                logger.error("heart beat check failure: " + server, e);
            } finally {
                if (socket != null && socket.isOpen()) {
                    socket.close();
                }

                if (!failure) {
                    newServers.add(server);
                }
            }
        }

        List<Service> services = new ArrayList<>();
        for (int i = 0; i < newServers.size(); i++) {
            Server server = newServers.get(i);
            Service s = new Service(server.getHost(), server.getPort(), "*", Service.PROVIDER_CATEGORY);

            services.add(s);
        }

        logger.info("flush service list " + services);
        notify(services);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            if (checkTimer != null) {
                checkTimer.cancel(true);
            }
        } catch (Throwable t) {
            logger.error("cancel check timer failure", t);
        }
    }

    public int getHeartbeatPeriod() {
        return heartbeatPeriod;
    }

    public void setHeartbeatPeriod(int heartbeatPeriod) {
        this.heartbeatPeriod = heartbeatPeriod;
    }

}

