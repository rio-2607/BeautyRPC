package com.beautyboss.slogen.rpc.config;

import com.beautyboss.slogen.rpc.common.Constants;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;

import java.io.File;

/**
 * Author : Slogen
 * Date   : 2019/1/27
 */
public class ConfigHelper {

    public static PropertiesConfiguration getProperties() throws ConfigurationException {
        /**
         * 首先尝试读取thrift-properties文件。如果文件不存在，
         * 则读取application.properties及对应的环境变量文件application-{env}.properties
         * 对于相同的key,application-{env}.properties会覆盖application.properties的设置
         */
        PropertiesConfiguration prop;
        if(exists(Constants.DEFAULT_CONFIG_PROPERTIES)) {
            prop = new PropertiesConfiguration(Constants.DEFAULT_CONFIG_PROPERTIES);
        } else {
            prop = new PropertiesConfiguration(Constants.CONFIG_PROPERTIES);
            String env = prop.getString(Constants.SPRINGBOOT_ENV);
            if(StringUtils.isNotEmpty(env) && exists(String.format(Constants.SPRINGBOOT_ENV_PROPERTIES,env))) {
                PropertiesConfiguration envProp = new PropertiesConfiguration(String.format(Constants.SPRINGBOOT_ENV_PROPERTIES,env));
                prop.copy(envProp);
            }
        }
        return prop;
    }


    private static boolean exists(String fileName) {
        File file = new File(fileName);
        return file.exists();
    }

}
