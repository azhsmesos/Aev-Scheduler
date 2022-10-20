package com.github.transfer.client;

import com.github.transfer.codec.Decoder;
import com.github.transfer.codec.Encoder;
import com.github.transfer.codec.Request;
import com.github.transfer.codec.Response;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-19
 */
public class ClientInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline cp = socketChannel.pipeline();
        cp.addLast(new Encoder(Request.class));
        cp.addLast(new Decoder(Response.class));
        cp.addLast(new ClientHandler());
        cp.addLast(new LengthFieldBasedFrameDecoder(65535, 0, 4, 0, 0));
    }
}
