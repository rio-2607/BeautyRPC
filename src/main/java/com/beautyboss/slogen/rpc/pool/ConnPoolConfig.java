package com.beautyboss.slogen.rpc.pool;

import com.beautyboss.slogen.rpc.common.Constants;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class ConnPoolConfig {
    public int maxIdle;
    public boolean testOnCreate;
    public boolean testOnBorrow;
    public boolean testOnReturn;

    public ConnPoolConfig() {
    }

    public ConnPoolConfig(PropertiesConfiguration prop) {
        maxIdle = prop.getInt(Constants.CONFIG_CLIENT_POOL_MAXIDLE, Constants.DEFAULT_MAX_IDLE);
        if (maxIdle < 0) {
            throw new IllegalArgumentException("maxIdle must >= 0");
        }
        testOnCreate = prop.getBoolean(Constants.CONFIG_CLIENT_POOL_TESTONCREATE, Constants.DEFAULT_TEST_ON_CREATE);

        testOnBorrow = prop.getBoolean(Constants.CONFIG_CLIENT_POOL_TESTONBORROW, Constants.DEFAULT_TEST_ON_BORROW);

        testOnReturn = prop.getBoolean(Constants.CONFIG_CLIENT_POOL_TESTONRETURN, Constants.DEFAULT_TEST_ON_RETURN);
    }
}
