package com.beautyboss.slogen.rpc.service;

import org.apache.commons.lang.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class Service {
    public final static String PROVIDER_CATEGORY = "providers";
    public final static String CONSUMER_CATEGORY = "consumers";

    public String host;
    public int port;
    public String name;
    public String category;
    private Map<String, String> parameters;

    // cache
    private String string;

    public Service(String host, int port, String name, String category) {
        this(host, port, name, category, (Map<String, String>) null);
    }

    public Service(String host, int port, String name, String category, String... pairs) {
        this(host, port, name, category, toStringMap(pairs));
    }

    public Service(String host, int port, String name, String category, Map<String, String> parameters) {
        if (host == null || name == null || category == null
                || port <= 0 || port > 65535) {
            throw new IllegalArgumentException("invalid args");
        }

        this.host = host;
        this.port = port;
        this.name = name;
        this.category = category;
        if (parameters != null) {
            this.parameters = new HashMap<>(parameters);
        }
    }

    public static Service valueOf(String service) {
        service = decode(service);
        String[] tmp = StringUtils.split(service, ':');
        if (tmp.length == 4) {
            return new Service(tmp[0], Integer.parseInt(tmp[1]), tmp[2], tmp[3]);
        } else if (tmp.length == 5) {
            return new Service(tmp[0], Integer.parseInt(tmp[1]), tmp[2], tmp[3], kvToStringMap(tmp[4]));
        } else {
            return null;
        }
    }


    public String getKey() {
        return host + ":" + port;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getParameterAndDecoded(String key) {
        return getParameterAndDecoded(key, null);
    }

    public String getParameterAndDecoded(String key, String defaultValue) {
        return decode(getParameter(key, defaultValue));
    }

    public String getParameter(String key) {
        return parameters.get(key);
    }

    public String getParameter(String key, String defaultValue) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return value;
    }

    public double getParameter(String key, double defaultValue) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        double d = Double.parseDouble(value);
        return d;
    }

    public long getParameter(String key, long defaultValue) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        long l = Long.parseLong(value);
        return l;
    }

    public int getParameter(String key, int defaultValue) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        int i = Integer.parseInt(value);
        return i;
    }

    public static Map<String, String> kvToStringMap(String kvs) {
        Map<String, String> parameters = new HashMap<String, String>();
        String[] fields = StringUtils.split(kvs, '&');
        for (int i = 0; i < fields.length; i++) {
            String[] kv = StringUtils.split(fields[i], '=');
            parameters.put(kv[0], kv[1]);
        }

        return parameters;
    }

    public static Map<String, String> toStringMap(String... pairs) {
        Map<String, String> parameters = new HashMap<String, String>();
        if (pairs.length > 0) {
            if (pairs.length % 2 != 0) {
                throw new IllegalArgumentException("pairs must be even.");
            }
            for (int i = 0; i < pairs.length; i = i + 2) {
                parameters.put(pairs[i], pairs[i + 1]);
            }
        }
        return parameters;
    }

    public static String encode(String value) {
        if (value == null || value.length() == 0) {
            return "";
        }
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static String decode(String value) {
        if (value == null || value.length() == 0) {
            return "";
        }
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void buildParameters(StringBuilder buf) {
        if (getParameters() != null && getParameters().size() > 0) {
            for (Map.Entry<String, String> entry : new TreeMap<String, String>(getParameters()).entrySet()) {
                if (entry.getKey() != null && entry.getKey().length() > 0) {
                    buf.append(entry.getKey());
                    buf.append("=");
                    buf.append(entry.getValue() == null ? "" : entry.getValue().trim());
                    buf.append('&');
                }
            }
        }
    }

    @Override
    public String toString() {
        if (string != null) {
            return string;
        }

        StringBuilder buf = new StringBuilder();
        buf.append(host);
        buf.append(':');
        buf.append(port);
        buf.append(':');
        buf.append(name);
        buf.append(':');
        buf.append(category);
        buf.append(':');
        buildParameters(buf);
        buf.setLength(buf.length() - 1);
        string = buf.toString();
        return string;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + port;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((category == null) ? 0 : category.hashCode());
        result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Service other = (Service) obj;
        if (host == null) {
            if (other.host != null)
                return false;
        } else if (!host.equals(other.host))
            return false;

        if (port != other.port)
            return false;

        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;

        if (category == null) {
            if (other.category != null)
                return false;
        } else if (!category.equals(other.category))
            return false;

        if (parameters == null) {
            if (other.parameters != null)
                return false;
        } else if (!parameters.equals(other.parameters))
            return false;

        return true;
    }

}
