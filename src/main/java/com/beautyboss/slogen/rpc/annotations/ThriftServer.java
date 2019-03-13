package com.beautyboss.slogen.rpc.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ThriftServer {


    /**
     * 服务分组
     *
     * @return
     */
    String group() default "default";

    /**
     * 是否注册到zk上面去
     *
     * @return
     */
    boolean register() default true;


}
