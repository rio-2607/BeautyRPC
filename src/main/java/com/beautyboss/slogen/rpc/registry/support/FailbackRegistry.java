package com.beautyboss.slogen.rpc.registry.support;

import com.beautyboss.slogen.rpc.Service;
import com.beautyboss.slogen.rpc.common.ConcurrentHashSet;
import com.beautyboss.slogen.rpc.common.Constants;
import com.beautyboss.slogen.rpc.common.NamedThreadFactory;
import com.beautyboss.slogen.rpc.registry.NotifyListener;

import java.util.*;
import java.util.concurrent.*;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public abstract class FailbackRegistry extends AbstractRegistry {
    // 定时任务执行器
    private final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory(
            "ThriftRegistryFailedRetryTimer", true));

    // 失败重试定时器，定时检查是否有请求失败，如有，无限次重试
    private ScheduledFuture<?> retryFuture;

    private final Set<Service> failedRegistered = new ConcurrentHashSet<Service>();

    private final Set<Service> failedUnregistered = new ConcurrentHashSet<Service>();

    private final ConcurrentMap<String, Set<NotifyListener>> failedSubscribed = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Set<NotifyListener>> failedUnsubscribed = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Map<NotifyListener, List<Service>>> failedNotified = new ConcurrentHashMap<>();

    protected int retryPeriod = Constants.DEFAULT_RETRY_PERIOD;
    protected boolean checkWhenStartup = Constants.DEFAULT_CHECK_WHEN_STARTUP;

    public void init()
    {
        logger.debug("init failback register");
        this.retryFuture = retryExecutor.scheduleWithFixedDelay(new Runnable() {
            public void run()
            {
                // 检测并连接注册中心
                try {
                    retry();
                } catch (Throwable t) { // 防御性容错
                    logger.error("Unexpected error occur at failed retry, cause: " + t.getMessage(), t);
                }
            }
        }, retryPeriod, retryPeriod, TimeUnit.MILLISECONDS);
    }

    public Future<?> getRetryFuture()
    {
        return retryFuture;
    }

    public Set<Service> getFailedRegistered()
    {
        return failedRegistered;
    }

    public Set<Service> getFailedUnregistered()
    {
        return failedUnregistered;
    }

    public Map<String, Set<NotifyListener>> getFailedSubscribed()
    {
        return failedSubscribed;
    }

    public Map<String, Set<NotifyListener>> getFailedUnsubscribed()
    {
        return failedUnsubscribed;
    }

    public Map<String, Map<NotifyListener, List<Service>>> getFailedNotified()
    {
        return failedNotified;
    }

    private void addFailedSubscribed(String service, NotifyListener listener)
    {
        Set<NotifyListener> listeners = failedSubscribed.get(service);
        if (listeners == null) {
            failedSubscribed.putIfAbsent(service, new ConcurrentHashSet<NotifyListener>());
            listeners = failedSubscribed.get(service);
        }
        listeners.add(listener);
    }

    private void removeFailedSubscribed(String service, NotifyListener listener)
    {
        Set<NotifyListener> listeners = failedSubscribed.get(service);
        if (listeners != null) {
            listeners.remove(listener);
        }
        listeners = failedUnsubscribed.get(service);
        if (listeners != null) {
            listeners.remove(listener);
        }
        Map<NotifyListener, List<Service>> notified = failedNotified.get(service);
        if (notified != null) {
            notified.remove(listener);
        }
    }

    @Override
    public void register(Service service)
    {
        super.register(service);
        failedRegistered.remove(service);
        failedUnregistered.remove(service);
        try {
            // 向服务器端发送注册请求
            doRegister(service);
        } catch (Exception e) {
            Throwable t = e;

            // 如果开启了启动时检测，则直接抛出异常
            boolean check = isCheckWhenStartup();
            if (check) {
                throw new IllegalStateException("Failed to register " + service + " to registry cause: "
                        + t.getMessage(), t);
            } else {
                logger.error("Failed to register " + service + ", waiting for retry, cause: " + t.getMessage(), t);
            }

            // 将失败的注册请求记录到失败列表，定时重试
            failedRegistered.add(service);
        }
    }

    @Override
    public void unregister(Service service)
    {
        super.unregister(service);
        failedRegistered.remove(service);
        failedUnregistered.remove(service);
        try {
            // 向服务器端发送取消注册请求
            doUnregister(service);
        } catch (Exception e) {
            Throwable t = e;

            // 如果开启了启动时检测，则直接抛出异常
            boolean check = isCheckWhenStartup();
            if (check) {
                throw new IllegalStateException("Failed to unregister " + service + " to registry cause: "
                        + t.getMessage(), t);
            } else {
                logger.error("Failed to uregister " + service + ", waiting for retry, cause: " + t.getMessage(), t);
            }

            // 将失败的取消注册请求记录到失败列表，定时重试
            failedUnregistered.add(service);
        }
    }

    @Override
    public void subscribe(String service, NotifyListener listener)
    {
        super.subscribe(service, listener);
        removeFailedSubscribed(service, listener);
        try {
            // 向服务器端发送订阅请求
            doSubscribe(service, listener);
        } catch (Exception e) {
            Throwable t = e;

            // 如果开启了启动时检测，则直接抛出异常
            boolean check = isCheckWhenStartup();
            if (check) {
                throw new IllegalStateException("Failed to subscribe " + service + ", cause: " + t.getMessage(), t);
            } else {
                logger.error("Failed to subscribe " + service + ", waiting for retry, cause: " + t.getMessage(), t);
            }

            // 将失败的订阅请求记录到失败列表，定时重试
            addFailedSubscribed(service, listener);
        }
    }

    @Override
    public void unsubscribe(String service, NotifyListener listener)
    {
        super.unsubscribe(service, listener);
        removeFailedSubscribed(service, listener);
        try {
            // 向服务器端发送取消订阅请求
            doUnsubscribe(service, listener);
        } catch (Exception e) {
            Throwable t = e;

            // 如果开启了启动时检测，则直接抛出异常
            boolean check = isCheckWhenStartup();
            if (check) {
                throw new IllegalStateException("Failed to unsubscribe " + service + " to registry cause: "
                        + t.getMessage(), t);
            } else {
                logger.error("Failed to unsubscribe " + service + ", waiting for retry, cause: " + t.getMessage(), t);
            }

            // 将失败的取消订阅请求记录到失败列表，定时重试
            Set<NotifyListener> listeners = failedUnsubscribed.get(service);
            if (listeners == null) {
                failedUnsubscribed.putIfAbsent(service, new ConcurrentHashSet<NotifyListener>());
                listeners = failedUnsubscribed.get(service);
            }
            listeners.add(listener);
        }
    }

    @Override
    protected void notify(String service, NotifyListener listener, List<Service> services)
    {
        if (service == null) {
            throw new IllegalArgumentException("notify service == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("notify listener == null");
        }
        try {
            doNotify(service, listener, services);
        } catch (Exception t) {
            // 将失败的通知请求记录到失败列表，定时重试
            Map<NotifyListener, List<Service>> listeners = failedNotified.get(service);
            if (listeners == null) {
                failedNotified.putIfAbsent(service, new ConcurrentHashMap<NotifyListener, List<Service>>());
                listeners = failedNotified.get(service);
            }
            listeners.put(listener, services);
            logger.error("Failed to notify for subscribe " + service + ", waiting for retry, cause: " + t.getMessage(),
                    t);
        }
    }

    protected void doNotify(String service, NotifyListener listener, List<Service> services)
    {
        super.notify(service, listener, services);
    }

    @Override
    protected void recover() throws Exception
    {
        logger.debug("recover registers and subscribes");

        // register
        Set<Service> recoverRegistered = new HashSet<Service>(getRegistered());
        if (!recoverRegistered.isEmpty()) {
            logger.info("Recover register service " + recoverRegistered);
            for (Service service : recoverRegistered) {
                failedRegistered.add(service);
            }
        }
        // subscribe
        Map<String, Set<NotifyListener>> recoverSubscribed = new HashMap<>(getSubscribed());
        if (!recoverSubscribed.isEmpty()) {
            logger.info("Recover subscribe service " + recoverSubscribed.keySet());
            for (Map.Entry<String, Set<NotifyListener>> entry : recoverSubscribed.entrySet()) {
                String service = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    addFailedSubscribed(service, listener);
                }
            }
        }
    }

    // 重试失败的动作
    protected void retry()
    {
        logger.debug("registry retry check");

        if (!failedRegistered.isEmpty()) {
            Set<Service> failed = new HashSet<>(failedRegistered);
            if (failed.size() > 0) {
                logger.info("Retry register " + failed);
                try {
                    for (Service service : failed) {
                        try {
                            doRegister(service);
                            failedRegistered.remove(service);
                        } catch (Throwable t) { // 忽略所有异常，等待下次重试
                            logger.warn(
                                    "Failed to retry register " + failed + ", waiting for again, cause: "
                                            + t.getMessage(), t);
                        }
                    }
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    logger.warn("Failed to retry register " + failed + ", waiting for again, cause: " + t.getMessage(),
                            t);
                }
            }
        }
        if (!failedUnregistered.isEmpty()) {
            Set<Service> failed = new HashSet<Service>(failedUnregistered);
            if (failed.size() > 0) {
                logger.info("Retry unregister " + failed);
                try {
                    for (Service service : failed) {
                        try {
                            doUnregister(service);
                            failedUnregistered.remove(service);
                        } catch (Throwable t) { // 忽略所有异常，等待下次重试
                            logger.warn(
                                    "Failed to retry unregister  " + failed + ", waiting for again, cause: "
                                            + t.getMessage(), t);
                        }
                    }
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    logger.warn(
                            "Failed to retry unregister  " + failed + ", waiting for again, cause: " + t.getMessage(),
                            t);
                }
            }
        }
        if (!failedSubscribed.isEmpty()) {
            Map<String, Set<NotifyListener>> failed = new HashMap<>(failedSubscribed);
            for (Map.Entry<String, Set<NotifyListener>> entry : new HashMap<>(failed).entrySet()) {
                if (entry.getValue() == null || entry.getValue().size() == 0) {
                    failed.remove(entry.getKey());
                }
            }
            if (failed.size() > 0) {
                logger.info("Retry subscribe " + failed);
                try {
                    for (Map.Entry<String, Set<NotifyListener>> entry : failed.entrySet()) {
                        String service = entry.getKey();
                        Set<NotifyListener> listeners = entry.getValue();
                        for (NotifyListener listener : listeners) {
                            try {
                                doSubscribe(service, listener);
                                listeners.remove(listener);
                            } catch (Throwable t) { // 忽略所有异常，等待下次重试
                                logger.warn(
                                        "Failed to retry subscribe " + failed + ", waiting for again, cause: "
                                                + t.getMessage(), t);
                            }
                        }
                    }
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    logger.warn(
                            "Failed to retry subscribe " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                }
            }
        }
        if (!failedUnsubscribed.isEmpty()) {
            Map<String, Set<NotifyListener>> failed = new HashMap<>(failedUnsubscribed);
            for (Map.Entry<String, Set<NotifyListener>> entry : new HashMap<>(failed).entrySet()) {
                if (entry.getValue() == null || entry.getValue().size() == 0) {
                    failed.remove(entry.getKey());
                }
            }
            if (failed.size() > 0) {
                logger.info("Retry unsubscribe " + failed);
                try {
                    for (Map.Entry<String, Set<NotifyListener>> entry : failed.entrySet()) {
                        String service = entry.getKey();
                        Set<NotifyListener> listeners = entry.getValue();
                        for (NotifyListener listener : listeners) {
                            try {
                                doUnsubscribe(service, listener);
                                listeners.remove(listener);
                            } catch (Throwable t) { // 忽略所有异常，等待下次重试
                                logger.warn("Failed to retry unsubscribe " + failed + ", waiting for again, cause: "
                                        + t.getMessage(), t);
                            }
                        }
                    }
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    logger.warn(
                            "Failed to retry unsubscribe " + failed + ", waiting for again, cause: " + t.getMessage(),
                            t);
                }
            }
        }
        if (!failedNotified.isEmpty()) {
            Map<String, Map<NotifyListener, List<Service>>> failed = new HashMap<>(failedNotified);
            for (Map.Entry<String, Map<NotifyListener, List<Service>>> entry : new HashMap<>(failed).entrySet()) {
                if (entry.getValue() == null || entry.getValue().size() == 0) {
                    failed.remove(entry.getKey());
                }
            }
            if (failed.size() > 0) {
                logger.info("Retry notify " + failed);
                try {
                    for (Map<NotifyListener, List<Service>> values : failed.values()) {
                        for (Map.Entry<NotifyListener, List<Service>> entry : values.entrySet()) {
                            try {
                                NotifyListener listener = entry.getKey();
                                List<Service> services = entry.getValue();
                                listener.notify(services);
                                values.remove(listener);
                            } catch (Throwable t) { // 忽略所有异常，等待下次重试
                                logger.warn(
                                        "Failed to retry notify " + failed + ", waiting for again, cause: "
                                                + t.getMessage(), t);
                            }
                        }
                    }
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    logger.warn("Failed to retry notify " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                }
            }
        }
    }

    @Override
    public void destroy()
    {
        super.destroy();
        try {
            retryFuture.cancel(true);
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
    }

    // ==== 模板方法 ====

    protected abstract void doRegister(Service service);

    protected abstract void doUnregister(Service service);

    protected abstract void doSubscribe(String service, NotifyListener listener);

    protected abstract void doUnsubscribe(String service, NotifyListener listener);

    public int getRetryPeriod()
    {
        return retryPeriod;
    }

    public void setRetryPeriod(int retryPeriod)
    {
        this.retryPeriod = retryPeriod;
    }

    public boolean isCheckWhenStartup()
    {
        return checkWhenStartup;
    }

    public void setCheckWhenStartup(boolean checkWhenStartup)
    {
        this.checkWhenStartup = checkWhenStartup;
    }
}
