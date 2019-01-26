package com.beautyboss.slogen.rpc.lb;

import com.beautyboss.slogen.rpc.Service;

import java.util.List;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public interface LoadBalance {
    public static final String  LOADBALANCE_RR  = "roundrobin";
    public static final String  LOADBALANCE_RAND  = "random";
    public static final String  LOADBALANCE_BROADCASE  = "broadcast";

    public Service select(List<Service> services);
}
