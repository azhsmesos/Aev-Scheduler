package com.github.transfer.client;

import com.github.transfer.client.proxy.AsyncProxy;
import com.github.transfer.client.proxy.ProxyImpl;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-19
 */
public class Client {

    private String serverAddress;

    private long timeout;

    private final Map<Class<?>, Object> syncProxyMap = new ConcurrentHashMap<>();

    private final Map<Class<?>, Object> asyncProxyMap = new ConcurrentHashMap<>();

    public void initClient(String serverAddress, long timeout) {
        this.serverAddress = serverAddress;
        this.timeout = timeout;
        connect();
    }

    private void connect() {
        ConnectManager.ConnectManagerEnum.RPC_CONNECT_MANAGER.getInstance().connect(serverAddress);
    }

    public void stop() {
        ConnectManager.ConnectManagerEnum.RPC_CONNECT_MANAGER.getInstance().stop();
    }

    public <T> T invokeSync(Class<T> interfaceClass) {
        if (syncProxyMap.containsKey(interfaceClass)) {
            return (T) syncProxyMap.get(interfaceClass);
        }
        Object obj = Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass},
                new ProxyImpl<>(interfaceClass, timeout));
        syncProxyMap.put(interfaceClass, obj);
        return (T) obj;
    }

    public <T> AsyncProxy invokeAsync(Class<T> interfaceClass) {
        if (asyncProxyMap.containsKey(interfaceClass)) {
            return (AsyncProxy) asyncProxyMap.get(interfaceClass);
        }
        ProxyImpl<T> asyncProxy = new ProxyImpl<>(interfaceClass, timeout);
        asyncProxyMap.put(interfaceClass, asyncProxy);
        return asyncProxy;
    }
}
