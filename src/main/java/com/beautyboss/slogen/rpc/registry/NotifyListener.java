package com.beautyboss.slogen.rpc.registry;

import com.beautyboss.slogen.rpc.service.Service;

import java.util.List;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public interface NotifyListener {

    void notify(List<Service> services);

}
