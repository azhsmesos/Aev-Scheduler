package com.github.transfer.client;

import com.github.transfer.codec.Request;
import com.github.transfer.codec.Response;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-19
 */
public class ClientHandler extends SimpleChannelInboundHandler<Response> {

    private Channel channel;

    private SocketAddress socketAddress;

    private final Map<String, RpcFuture> localRpcTable = new ConcurrentHashMap<>();

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.socketAddress = this.channel.remoteAddress();
        this.channel = ctx.channel();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Response resp) throws Exception {
        String requestID = resp.getRequestID();
        RpcFuture future = localRpcTable.get(requestID);
        if (future != null) {
            localRpcTable.remove(requestID);
            future.done(resp);
        }
    }

    public SocketAddress getAddress() {
        return this.socketAddress;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public void close() {
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    public RpcFuture sendRequest(Request request) {
        RpcFuture future = new RpcFuture(request);
        localRpcTable.put(request.getRequestID(), future);
        channel.writeAndFlush(request);
        return future;
    }
}
