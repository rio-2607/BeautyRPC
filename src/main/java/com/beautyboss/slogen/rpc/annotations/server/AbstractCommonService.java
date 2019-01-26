package com.beautyboss.slogen.rpc.annotations.server;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public abstract class AbstractCommonService extends Thread {
    private static Logger logger = LoggerFactory.getLogger(AbstractCommonService.class);
    protected volatile boolean IS_STOP = false;

    public AbstractCommonService(String threadName) {
        super(threadName);
    }

    protected abstract void shutdown();

    public boolean check() {
        return this.isAlive();
    }

    @Override
    public void interrupt() {
        this.IS_STOP = true;
        try {
            this.shutdown();
        } catch (Exception e) {
            //do nothing
        }
        try {
            super.interrupt();
        } catch (Exception e) {
            logger.info(ExceptionUtils.getMessage(e));
        }
    }


}

