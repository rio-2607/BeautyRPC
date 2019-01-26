package com.beautyboss.slogen.rpc.annotations.server;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class ServiceGroup {

    private Logger logger = LoggerFactory.getLogger(ServiceGroup.class);

    private List<Thread> serviceList = new ArrayList<>();

    private String name; // 服务组名称

    public <T extends Thread> void add(T service) {
        this.serviceList.add(service);
    }

    public void start() {
        logger.info("service group '{}' now starting...", this.name);
        if (this.serviceList.isEmpty()) {
            logger.warn("service group '{}' is empty.", this.name);
            return;
        }
        for (Thread thread : serviceList) {
            logger.info("now start {}", thread.getName());
            thread.start();
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.sleep(500);
                    if (thread.isAlive()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    logger.warn("start {} service error, message: {}", thread.getName(),
                            ExceptionUtils.getStackTrace(e));
                    break;
                }
            }
            // 检查服务是不是真的启动了
            if (thread.isAlive()) {
                logger.info("start {} complete", thread.getName());
            } else {
                logger.info("start {} failed, we will shutdown all service",
                        thread.getName());
                this.shutdown();
                break;
            }
        }
        logger.info("service group '{}' start complete.", this.name);
    }

    /**
     * 检查服务状态
     *
     * @return true=状态都正常，false代表有错误状态
     */
    public boolean check() {
        logger.info("check service group '{}' ...", this.name);
        if (this.serviceList.isEmpty()) { // 没有任何服务，直接返回错误
            logger.warn("service group '{}' is empty.", this.name);
            return false;
        }
        StringBuilder sb = new StringBuilder();
        boolean allAlive = true;
        for (Thread thread : this.serviceList) {
            boolean isAlive = thread.isAlive();
            if (isAlive == false && allAlive == true) {
                allAlive = false;
            }
            sb.append("[").append(thread.getName()).append("]")
                    .append(isAlive ? "ALIVE" : "DIE").append(", ");
        }
        logger.info(sb.toString());
        return allAlive;
    }

    public void shutdown() {
        logger.info("service group '{}' shutdown ...", this.name);
        if (serviceList.isEmpty()) {
            logger.info("service group is empty, not need to shut down any service.");
            return;
        }
        for (Thread thread : serviceList) {
            logger.info("begin show down {} ...", thread.getName());
            if (!thread.isAlive()) {
                logger.info("service '{}' already not alive", thread.getName());
            }
            thread.interrupt();
            for (int i = 0; i < 10; i++) {
                try {
                    thread.join(1000);
                } catch (InterruptedException e) {
                    logger.warn("error find when shutdown {}, message: {}",
                            thread.getName(), ExceptionUtils.getMessage(e));
                    break;
                }
            }
            // 检查是否正常关闭服务了
            if (thread.isAlive()) {
                // 还是alive状态，表示过了10s还是无法关闭，直接报个waring吧
                logger.warn("{} is still alive, please shutdown yourself",
                        thread.getName());
            }
        }
        logger.info("service group '{}' shutdown complete.", this.name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setServiceList(List<Thread> serviceList) {
        this.serviceList = serviceList;
    }
}
