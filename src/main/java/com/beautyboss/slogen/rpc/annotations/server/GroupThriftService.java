package com.beautyboss.slogen.rpc.annotations.server;

import com.beautyboss.slogen.rpc.service.thrift.ThriftService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class GroupThriftService extends AbstractCommonService {

    private String serviceName;

    private static Logger logger = LoggerFactory.getLogger(GroupThriftService.class);

    private ThriftService thriftService;

    public GroupThriftService(String serviceName) {
        super(serviceName);
        this.serviceName = serviceName;
    }

    @Override
    public void run() {
        try {
            logger.info("begin start {} service...", serviceName);
            this.thriftService.init();
            logger.info("thrift service exit.");
        } catch (Exception e) {
            if (!IS_STOP) {
                logger.error("server start error!!!, message = {}",
                        org.apache.commons.lang.exception.ExceptionUtils.getMessage(e));
            }
            this.shutdown();
        }
    }

    @Override
    protected void shutdown() {
        logger.info("service {} shutdown.", serviceName);
    }

    public void setThriftService(ThriftService thriftService) {
        this.thriftService = thriftService;
    }
}
