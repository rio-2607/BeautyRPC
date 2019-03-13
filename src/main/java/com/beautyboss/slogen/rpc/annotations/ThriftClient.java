package com.beautyboss.slogen.rpc.annotations;

import org.springframework.beans.factory.annotation.Autowired;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
/**
 * 注意：这里不能使用Resource注解，只能用Autowired注解
 * Resource默认是byName注入，Autowired是byType注入
 */
@Autowired
public @interface ThriftClient {

    /**
     * 是否从zk获取 默认是
     *
     * @return
     */
    boolean register() default true;

    /**
     * 直连地址
     *
     * @return
     */
    String upstreamAddrs() default "";

    /**
     * 服务端是否支持multi processor 默认true
     */
    boolean isMulti() default true;


}
