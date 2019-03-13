package com.beautyboss.slogen.rpc.lb;

import com.beautyboss.slogen.rpc.service.Service;

import java.util.List;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public interface LoadBalance {
    String  LOADBALANCE_RR  = "roundrobin";
    String  LOADBALANCE_RAND  = "random";
    String  LOADBALANCE_BROADCASE  = "broadcast";

    Service select(List<Service> services);
}
