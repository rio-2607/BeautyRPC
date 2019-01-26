package com.beautyboss.slogen.rpc.annotations.model;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public abstract class AbstractThriftModel {

    public abstract void register(Class<? extends Annotation> annotationClass, BeanDefinitionRegistry Registry, Set<BeanDefinitionHolder> beanDefinitions);

}
