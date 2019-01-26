package com.beautyboss.slogen.rpc;

import com.beautyboss.slogen.rpc.annotations.server.ServiceGroup;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class SpringThriftApplication {

    private final static Logger logger = LoggerFactory
            .getLogger(SpringThriftApplication.class);
    private static volatile AtomicBoolean IS_STOP = new AtomicBoolean(false);
    private static ServiceGroup sg;

    public static void run(Class source, String... args) {

        ApplicationContext context = SpringApplication.run(source, args);

        // 获取service group实例
        sg = context.getBean(ServiceGroup.class);

        logger.info("register shutdown hook");
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                IS_STOP.set(true); // 设置结束标志位true
                if (sg != null) {
                    sg.shutdown();
                }
            }
        }));

        logger.info("start service group ...");
        sg.start();

        while (!IS_STOP.get()) {
            logger.info("check service status...");
            if (sg.check()) { // 状态正常
                try {
                    logger.debug("service status OK.");
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    logger.warn(ExceptionUtils.getMessage(e));
                }
            } else {
                logger.info("service status ERROR.");
                IS_STOP.set(true);
                sg.shutdown();
            }
        }
        logger.info("service closed.");
    }
}
