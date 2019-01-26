package com.beautyboss.slogen.rpc.lb.impl;

import com.beautyboss.slogen.rpc.service.Service;
import com.beautyboss.slogen.rpc.lb.LoadBalance;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class RRLoadBalance implements LoadBalance {
    private AtomicInteger inc = new AtomicInteger(UUID.randomUUID().toString().hashCode());

    @Override
    public Service select(List<Service> services)
    {
        return services.get(Math.abs(inc.incrementAndGet()) % services.size());
    }
}
