package com.github.transfer.server;

import com.github.transfer.codec.Request;
import com.github.transfer.codec.Response;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-21
 */
@Slf4j
public class ServerHandler extends SimpleChannelInboundHandler<Request> {

    private final Map<String, Object> handlerMap;

    private final ThreadPoolExecutor executor =
            new ThreadPoolExecutor(16, 16, 600L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(65535));


    public ServerHandler(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Request request) throws Exception {
        executor.submit(() -> {
            Response resp = new Response();
            resp.setRequestID(request.getRequestID());
            try {
                resp.setResult(handle(request));
            } catch (Throwable throwable) {
                resp.setThrowable(throwable);
            }
            channelHandlerContext.writeAndFlush(resp).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (channelFuture.isSuccess()) {
                        // after rpc hook
                    }
                }
            });
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("server caught Throwable: " + cause);
        ctx.close();
    }

    private Object handle(Request request) throws InvocationTargetException {
        String className = request.getClassName();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();
        Object ref = handlerMap.get(className);
        Class<?> clz = ref.getClass();

        // cglib reflect
        FastClass fastClass = FastClass.create(clz);
        FastMethod fastMethod = fastClass.getMethod(methodName, parameterTypes);
        return fastMethod.invoke(ref, parameters);
    }
}
