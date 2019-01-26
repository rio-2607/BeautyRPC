package com.beautyboss.slogen.rpc.annotations.scanner;

import com.beautyboss.slogen.rpc.annotations.ThriftClient;
import com.beautyboss.slogen.rpc.annotations.ThriftServer;
import com.beautyboss.slogen.rpc.annotations.model.AbstractThriftModel;
import com.beautyboss.slogen.rpc.annotations.model.ThriftClientModel;
import com.beautyboss.slogen.rpc.annotations.model.ThriftServerModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class ThriftScanner extends ClassPathBeanDefinitionScanner {

    protected final Log logger = LogFactory.getLog(ThriftScanner.class);

    private Class<? extends Annotation> annotationClass;

    private String serviceGroupName;

    private ThriftScannerModel thriftModel;

    private AbstractThriftModel model;

    public ThriftScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
    }

    public void registerFilters() {

        model = getThriftScannerModel(thriftModel);

        switch (thriftModel) {
            case THRIFT_CLIENT_MODEL:
                // Include Annotation scan client
                addIncludeFilter(new TypeFilter() {

                    @Override
                    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
                            throws IOException {

                        ClassMetadata classMetadata = metadataReader.getClassMetadata();

                        if (classMetadata.isInterface() || classMetadata.isAbstract() || classMetadata.isAnnotation()) {

                            return false;
                        }

                        String className = classMetadata.getClassName();

                        try {

                            Class<?> clazz = org.springframework.util.ClassUtils.forName(className,
                                    getClass().getClassLoader());
                            Field[] fields = clazz.getDeclaredFields();
                            for (Field field : fields) {
                                if (field.isAnnotationPresent(annotationClass)) {
                                    return Boolean.TRUE;
                                }
                            }
                        } catch (Throwable ex) {
                            // Class not regularly loadable - can't determine a match that way.
                        }
                        return false;
                    }
                });
                break;
            case THRIFT_SERVER_MODEL:
                // Include Annotation scan server
                addIncludeFilter(new AnnotationTypeFilter(annotationClass));
                break;
            default:
                break;
        }
    }

    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {

        Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);
        if (beanDefinitions.isEmpty()) {
            logger.warn("No monitor class or method found in '" + Arrays.toString(basePackages)
                    + "' package. Please check your configuration.");
        } else {

            model.register(this.annotationClass,
                    getRegistry(),
                    beanDefinitions);
        }

        return beanDefinitions;
    }

    // 如果注册过的bean会被spring过滤掉
    @Override
    protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) throws IllegalStateException {

        return true;
    }

    public void setServiceGroupName(String serviceGroupName) {
        this.serviceGroupName = serviceGroupName;
    }

    public void setThriftModel(ThriftScannerModel thriftModel) {
        this.thriftModel = thriftModel;
    }

    private AbstractThriftModel getThriftScannerModel(ThriftScannerModel thriftModel) {

        switch (thriftModel) {
            case THRIFT_CLIENT_MODEL:
                this.annotationClass = ThriftClient.class;
                return new ThriftClientModel();
            case THRIFT_SERVER_MODEL:
                ThriftServerModel serverModel = new ThriftServerModel();
                serverModel.setServiceGroupName(serviceGroupName);
                this.annotationClass = ThriftServer.class;
                return serverModel;
            default:
                // 自动识别
                break;
        }
        return null;
    }
}
