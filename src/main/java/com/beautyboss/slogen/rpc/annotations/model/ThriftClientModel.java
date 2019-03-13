package com.beautyboss.slogen.rpc.annotations.model;

import com.beautyboss.slogen.rpc.annotations.ThriftClient;
import com.beautyboss.slogen.rpc.annotations.factory.ThriftClientFactory;
import com.beautyboss.slogen.rpc.client.thrift.ClientProxy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.thrift.transport.TFramedTransport;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class ThriftClientModel extends AbstractThriftModel {

    protected final Log logger = LogFactory.getLog(ThriftClientModel.class);

    @Override
    public void register(Class<? extends Annotation> annotationClass, BeanDefinitionRegistry registry, Set<BeanDefinitionHolder> beanDefinitions) {

        Map<Class<?>, ThriftClient> servers = new HashMap<>(beanDefinitions.size());

        for (BeanDefinitionHolder holder : beanDefinitions) {

            if (holder.getBeanDefinition() instanceof ScannedGenericBeanDefinition) {

                final ScannedGenericBeanDefinition beanDefinition = (ScannedGenericBeanDefinition) holder.getBeanDefinition();
                AnnotationMetadata metadata = beanDefinition.getMetadata();
                String thriftServiceClass = metadata.getClassName();
                try {

                    Class<?> thriftServiceClazz = org.springframework.util.ClassUtils.forName(thriftServiceClass,
                            getClass().getClassLoader());
                    Field[] fields = thriftServiceClazz.getDeclaredFields();
                    for (Field field : fields) {
                        if (field.isAnnotationPresent(annotationClass)) {
                            Class<?> type = field.getType();
                            String serviceClazz = type.getName().split("\\$Client")[0];
                            servers.put(org.springframework.util.ClassUtils.forName(serviceClazz,
                                    getClass().getClassLoader()),
                                    (ThriftClient) field.getAnnotation(annotationClass));
                        }
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
        }

        GenericBeanDefinition thriftService = new GenericBeanDefinition();
        thriftService.setBeanClassName(com.beautyboss.slogen.rpc.client.thrift.ThriftClient.class.getName());
        thriftService.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);
        thriftService.setDestroyMethodName("stop");
        thriftService.setInitMethodName("init");
        String thriftServiceName = BeanDefinitionReaderUtils.generateBeanName(thriftService, registry);

        ManagedList<BeanReference> thriftServiceBean = new ManagedList<>();

        for (Map.Entry<Class<?>, ThriftClient> entry : servers.entrySet()) {

            GenericBeanDefinition serviceProxy = new GenericBeanDefinition();
            serviceProxy.setBeanClassName(ClientProxy.class.getName());
            serviceProxy.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);
            ConstructorArgumentValues values = new ConstructorArgumentValues();
            values.addIndexedArgumentValue(0, entry.getKey());
            values.addIndexedArgumentValue(1, entry.getValue().isMulti());
            serviceProxy.setConstructorArgumentValues(values);
            serviceProxy.getPropertyValues().add("transportFactory", new TFramedTransport.Factory());
            serviceProxy.getPropertyValues().add("register", entry.getValue().register());
            serviceProxy.getPropertyValues().add("upstreamAddrs", entry.getValue().upstreamAddrs());
            String beanName = BeanDefinitionReaderUtils.generateBeanName(serviceProxy, registry);
            registry.registerBeanDefinition(beanName, serviceProxy);
            thriftServiceBean.add(new RuntimeBeanReference(beanName));

            GenericBeanDefinition thriftFactory = new GenericBeanDefinition();
            thriftFactory.setBeanClassName(ThriftClientFactory.class.getName());
            thriftFactory.getPropertyValues().add("serviceClazz", entry.getKey());
            thriftFactory.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);
            String thriftFactoryName = BeanDefinitionReaderUtils.generateBeanName(thriftFactory, registry);
            thriftFactory.getPropertyValues().add("thriftClient", new RuntimeBeanReference(thriftServiceName));
            registry.registerBeanDefinition(thriftFactoryName, thriftFactory);
        }

        thriftService.getPropertyValues().add("proxyList", thriftServiceBean);
        registry.registerBeanDefinition(thriftServiceName, thriftService);
    }
}
