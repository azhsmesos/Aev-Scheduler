package com.github.transfer.server;

import com.github.transfer.codec.Decoder;
import com.github.transfer.codec.Encoder;
import com.github.transfer.codec.Request;
import com.github.transfer.codec.Response;
import com.github.transfer.config.provider.Provider;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-21
 */
@Slf4j
public class Server {

    private final String serverAddress;

    // 接收链接
    private final EventLoopGroup bossGroup = new NioEventLoopGroup();

    // 具体处理逻辑
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private final Map<String, Object> handlerMap = new HashMap<>();

    public Server(String serverAddress) throws InterruptedException {
        this.serverAddress = serverAddress;
        this.start();
    }

    private void start() throws InterruptedException {
        ServerBootstrap strap = new ServerBootstrap();
        strap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                // backlogSize = sync + accept
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline cp = socketChannel.pipeline();
                        cp.addLast(new LengthFieldBasedFrameDecoder(65535, 0, 4, 0, 0));
                        cp.addLast(new Decoder(Request.class));
                        cp.addLast(new Encoder(Response.class));
                        cp.addLast(new ServerHandler(handlerMap));
                    }
                });
        String[] array = this.serverAddress.split(":");
        String host = array[0];
        int port = Integer.parseInt(array[1]);

        ChannelFuture future = strap.bind(host, port).sync();
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                    log.info("server success bing to: " + serverAddress);
                } else {
                    log.info("server fail bing to: " + serverAddress);
                    throw new RuntimeException("server start failed, cause: " + future.cause());
                }
            }
        });

        // sync 同步阻塞并等待
        future.await(5000, TimeUnit.MILLISECONDS);
        if (future.isSuccess()) {
            // bind success
            log.info("starter success");
        }

    }

    public void registerProcessor(Provider provider) {
        handlerMap.put(provider.getInterface(), provider.getRef());
    }

    public void close() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
