package com.beautyboss.slogen.rpc.lb.impl;

import com.beautyboss.slogen.rpc.Service;
import com.beautyboss.slogen.rpc.lb.LoadBalance;

import java.util.List;
import java.util.Random;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class RandomLoadBalance implements LoadBalance {
    private Random rand = new Random();

    @Override
    public Service select(List<Service> services)
    {
        return services.get(rand.nextInt(services.size()));
    }
}
