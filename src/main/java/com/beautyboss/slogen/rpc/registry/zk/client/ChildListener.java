package com.beautyboss.slogen.rpc.registry.zk.client;

import java.util.List;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public interface ChildListener {

    void childChanged(String path, List<String> children);

}
