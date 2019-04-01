package com.beautyboss.slogen.rpc.registry.sstatic;

import com.beautyboss.slogen.rpc.service.Server;
import com.beautyboss.slogen.rpc.service.Service;
import com.beautyboss.slogen.rpc.common.Constants;
import com.beautyboss.slogen.rpc.registry.NotifyListener;
import com.beautyboss.slogen.rpc.registry.support.AbstractRegistry;
import org.apache.commons.lang.StringUtils;
import org.apache.thrift.transport.TSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class StaticRegistry extends AbstractRegistry {
    private static final Logger logger = LoggerFactory.getLogger(StaticRegistry.class);

    /* init by framework */
    private List<Server> servers;

    public StaticRegistry() {
    }

    public StaticRegistry(String hosts, int heartbeatPeriod) {
        this.hosts = hosts;
        init();
    }

    public void init() {
        String addrs = getHosts();
        if (addrs == null || "".equals(addrs.trim())) {
            throw new IllegalStateException("static service address == null");
        }

        parseServer(addrs);
    }

    private void parseServer(String addr) {
        String[] srv = StringUtils.split(addr, ',');
        servers = Arrays.stream(srv).map(server -> {
            String[] info = StringUtils.split(server, ':');
            return new Server(info[0], Integer.parseInt(info[1]));
        }).collect(Collectors.toList());
    }

    public void subscribe(final String service, final NotifyListener listener) {
        super.subscribe(service, listener);
        List<Service> services = servers.stream()
                .map(server -> new Service(server.getHost(), server.getPort(), service, Service.PROVIDER_CATEGORY))
                .collect(Collectors.toList());

        notify(service, listener, services);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }


}

