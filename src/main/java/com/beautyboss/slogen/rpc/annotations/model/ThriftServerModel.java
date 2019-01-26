package com.beautyboss.slogen.rpc.annotations.model;

import com.beautyboss.slogen.rpc.annotations.ThriftServer;
import com.beautyboss.slogen.rpc.annotations.server.GroupThriftService;
import com.beautyboss.slogen.rpc.annotations.server.ServiceGroup;
import com.beautyboss.slogen.rpc.service.thrift.ServiceProxy;
import com.beautyboss.slogen.rpc.service.thrift.ThriftService;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class ThriftServerModel extends AbstractThriftModel {

    protected final Log logger = LogFactory.getLog(ThriftServerModel.class);

    private String serviceGroupName;

    @Override
    public void register(Class<? extends Annotation> annotationClass, BeanDefinitionRegistry registry, Set<BeanDefinitionHolder> beanDefinitions) {

        ManagedList<BeanReference> thriftServiceBean = new ManagedList<>();

        for (BeanDefinitionHolder holder : beanDefinitions) {

            if(holder.getBeanDefinition() instanceof ScannedGenericBeanDefinition){

                final ScannedGenericBeanDefinition beanDefinition = (ScannedGenericBeanDefinition)holder.getBeanDefinition();
                AnnotationMetadata metadata = beanDefinition.getMetadata();
                String thriftServiceClass = metadata.getClassName();
                try {

                    Class<?> thriftServiceClazz = ClassUtils.getClass(thriftServiceClass);

                    ThriftServer thriftService = (ThriftServer) thriftServiceClazz.getAnnotation(annotationClass);
                    //这里需要注意
                    List<Class<?>> allInterfaces = ClassUtils.getAllInterfaces(thriftServiceClazz);
                    String superClassName = null;
                    for (Class<?> clazz : allInterfaces) {
                        if(clazz.getName().endsWith("$Iface")) {
                            superClassName = clazz.getName().split("\\$Iface")[0];
                            break;
                        }
                    }
                    if(StringUtils.isBlank(superClassName)) {
                        continue;
                    }
                    BeanDefinition serviceProxy = new GenericBeanDefinition();
                    serviceProxy.setBeanClassName(ServiceProxy.class.getName());
                    serviceProxy.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);
                    serviceProxy.getPropertyValues().add("ifaceClass", ClassUtils.getClass(superClassName));
                    serviceProxy.getPropertyValues().add("ifaceImpl", new RuntimeBeanReference(holder.getBeanName()));
                    serviceProxy.getPropertyValues().add("register", thriftService.register());
                    String beanName = BeanDefinitionReaderUtils.generateBeanName(serviceProxy, registry);
                    registry.registerBeanDefinition(beanName, serviceProxy);
                    thriftServiceBean.add(new RuntimeBeanReference(beanName));
                    logger.info("registry thrift server " + thriftServiceClass);
                } catch (ClassNotFoundException e) {
                    logger.error("thrift server registry fail", e);
                    throw new RuntimeException(e.getMessage());
                }
            }
        }

        GenericBeanDefinition thriftService = new GenericBeanDefinition();
        thriftService.setBeanClassName(ThriftService.class.getName());
        thriftService.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);
        thriftService.getPropertyValues().add("services", thriftServiceBean);
        thriftService.setDestroyMethodName("stop");
        String thriftServiceName = BeanDefinitionReaderUtils.generateBeanName(thriftService, registry);
        registry.registerBeanDefinition(thriftServiceName, thriftService);

        GenericBeanDefinition groupThriftService = new GenericBeanDefinition();
        groupThriftService.setBeanClassName(GroupThriftService.class.getName());
        groupThriftService.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);
        groupThriftService.getPropertyValues().add("thriftService", new RuntimeBeanReference(thriftServiceName));
        ConstructorArgumentValues values = new ConstructorArgumentValues();
        values.addIndexedArgumentValue(0, this.serviceGroupName);
        groupThriftService.setConstructorArgumentValues(values);
        String couponThriftServiceName = BeanDefinitionReaderUtils.generateBeanName(groupThriftService, registry);
        registry.registerBeanDefinition(couponThriftServiceName, groupThriftService);

        ManagedList<BeanReference> serviceList = new ManagedList<>();
        serviceList.add(new RuntimeBeanReference(couponThriftServiceName));
        BeanDefinition serviceGroup = new GenericBeanDefinition();
        serviceGroup.setBeanClassName(ServiceGroup.class.getName());
        serviceGroup.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);
        serviceGroup.getPropertyValues().add("serviceList", serviceList);
        serviceGroup.getPropertyValues().add("name", this.serviceGroupName);
        String serviceGroupName = BeanDefinitionReaderUtils.generateBeanName(groupThriftService, registry);
        registry.registerBeanDefinition(serviceGroupName, serviceGroup);
    }

    public void setServiceGroupName(String serviceGroupName) {
        this.serviceGroupName = serviceGroupName;
    }
}
