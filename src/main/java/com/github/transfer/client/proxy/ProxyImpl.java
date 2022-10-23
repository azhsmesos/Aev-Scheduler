package com.github.transfer.client.proxy;

import com.github.transfer.client.ClientHandler;
import com.github.transfer.client.ConnectManager;
import com.github.transfer.client.RpcFuture;
import com.github.transfer.codec.Request;
import com.sun.source.tree.ReturnTree;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-23
 */
public class ProxyImpl<T> implements InvocationHandler, AsyncProxy {

    private Class<T> clz;

    private long timeout;

    public ProxyImpl(Class<T> clz, long timeout) {
        this.clz = clz;
        this.timeout = timeout;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Request request = new Request();
        request.setRequestID(UUID.randomUUID().toString());
        request.setClassName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(args);
        ConnectManager manager = ConnectManager.ConnectManagerEnum.RPC_CONNECT_MANAGER.getInstance();
        RpcFuture future = manager.chooseHandler().sendRequest(request);
        return future.get(timeout, TimeUnit.SECONDS);
    }

    @Override
    public RpcFuture call(String methodName, Object... args) {
        Request request = new Request();
        request.setRequestID(UUID.randomUUID().toString());
        request.setClassName(this.clz.getName());
        request.setMethodName(methodName);
        request.setParameters(args);
        Class<?>[] parameterTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = ClassType.INSTANCE.getClassType(args[i]);
        }
        request.setParameterTypes(parameterTypes);
        ClientHandler handler = ConnectManager.ConnectManagerEnum.RPC_CONNECT_MANAGER.getInstance().chooseHandler();
        return handler.sendRequest(request);
    }

    public enum ClassType {
        INSTANCE("INSTANCE"),
        INTEGER("java.lang.Integer"),
        LONG("java.lang.Long"),
        FLOAT("java.lang.Float"),
        DOUBLE("java.lang.Double"),
        CHARACTER("java.lang.Character"),
        BOOLEAN("java.lang.Boolean"),
        SHORT("java.lang.Short"),
        BYTE("java.lang.Byte");

        ClassType(String value) {
        }

        private Class<?> getClassType(Object obj) {
            Class<?> classType = obj.getClass();
            String typeName = classType.getName();
            String name = typeName.substring(typeName.lastIndexOf(".") + 1);
            switch (ClassType.valueOf(name.toUpperCase())) {
                case INTEGER -> {
                    return Integer.TYPE;
                }
                case LONG -> {
                    return Long.TYPE;
                }
                case FLOAT -> {
                    return Float.TYPE;
                }
                case DOUBLE -> {
                    return Double.TYPE;
                }
                case CHARACTER -> {
                    return Character.TYPE;
                }
                case BOOLEAN -> {
                    return Boolean.TYPE;
                }
                case SHORT -> {
                    return Short.TYPE;
                }
                case BYTE -> {
                    return Byte.TYPE;
                }
                default -> throw new RuntimeException("error types");
            }
        }
    }
}
