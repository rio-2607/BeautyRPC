package com.beautyboss.slogen.rpc.annotations.scanner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

import static org.springframework.util.Assert.notNull;

/**
 * Author : Slogen
 * Date   : 2019/1/27
 */
public class ThriftScannerConfig implements BeanDefinitionRegistryPostProcessor, InitializingBean, ApplicationContextAware {

    protected final Log logger = LogFactory.getLog(ThriftScannerConfig.class);

    /**
     * 需要扫描的包  多个逗号或者分号隔开
     */
    private String basePackage = "com.beautyboss.slogen";

    private ApplicationContext applicationContext;

    private ThriftScannerModel thriftModel;

    /**
     * 服务组名称
     */
    private String serviceGroupName = "default";

    @Override
    public void postProcessBeanFactory(
            ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        notNull(this.basePackage, "Property 'basePackage' is required");
        notNull(this.thriftModel, "Property 'thriftModel' is required");
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        ThriftScanner scanner = new ThriftScanner(registry);
        scanner.setResourceLoader(this.applicationContext);
        scanner.setServiceGroupName(this.serviceGroupName);
        scanner.setThriftModel(this.thriftModel);
        scanner.registerFilters();
        scanner.scan(StringUtils.tokenizeToStringArray(this.basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
    }

	/*public void setAnnotationClazz(String annotationClazz) {
		if(!StringUtils.isEmpty(annotationClazz)){
			StringBuffer sb = new StringBuffer(this.annotationClazz);
			sb.append(",").append(annotationClazz);
			this.annotationClazz = sb.toString();
		}
	}*/

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public void setServiceGroupName(String serviceGroupName) {
        this.serviceGroupName = serviceGroupName;
    }

    public void setThriftModel(ThriftScannerModel thriftModel) {
        this.thriftModel = thriftModel;
    }
}
