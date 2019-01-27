package com.beautyboss.slogen.rpc.annotations;

import javax.annotation.Resource;
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
@Resource
public @interface ThriftClient {

    /**
     * 是否从zk获取 默认是
     * @return
     */
    boolean register() default true;

    /**
     * 直连地址
     * @return
     */
    String upstreamAddrs() default "";

    /**
     * 服务端是否支持multi processor 默认true
     */
    boolean isMulti() default true;


}
