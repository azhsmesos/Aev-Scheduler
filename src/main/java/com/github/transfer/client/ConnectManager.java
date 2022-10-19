package com.github.transfer.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-19
 *
 * @Document rpc connect manager
 */
@Slf4j
public class ConnectManager {

    private Map<SocketAddress, ClientHandler> connectedHandlerMap = new ConcurrentHashMap<>();

    private CopyOnWriteArrayList<ClientHandler> connectedhandlerList = new CopyOnWriteArrayList<>();

    private ReentrantLock connectedLock = new ReentrantLock();
    private Condition connectedCondition = connectedLock.newCondition();

    private long connectedTimeoutMills = 6000;
    private volatile boolean isRunning = true;

    private AtomicInteger handlerIndex = new AtomicInteger(0);

    private ConnectManager() {}

    private ThreadPoolExecutor asyncSubmitConnectedExecutor =
            new ThreadPoolExecutor(16, 16, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(65536));

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);


    public void connect(final String serverAddress) {
        List<String> allServerAddress = Arrays.asList(serverAddress.split(","));
        updateConnectedServer(allServerAddress);
    }

    /**
     * 更新cache信息，异步发起链接
     * @param allServerAddress [localhost1:port1,localhost2:port2]
     */
    public void updateConnectedServer(List<String> allServerAddress) {
        if (CollectionUtils.isNotEmpty(allServerAddress)) {
            Set<SocketAddress> newAllServerAddress = new HashSet<>();
            for (int i = 0; i < allServerAddress.size(); i++) {
                String[] array = allServerAddress.get(i).trim().split(":");
                if (array.length == 2) {
                    String host = array[0].trim();
                    int port = Integer.parseInt(array[1].trim());
                    final InetSocketAddress remotePeer = new InetSocketAddress(host, port);
                    newAllServerAddress.add(remotePeer);
                }
            }

            for (SocketAddress address : newAllServerAddress) {
                if (connectedHandlerMap.containsKey(address)) {
                    continue;
                }
                connectAsync(address);
            }

            for (int i = 0; i < connectedhandlerList.size(); i++) {
                ClientHandler handler = connectedhandlerList.get(i);
                SocketAddress address = handler.getAddress();
                if (!newAllServerAddress.contains(address)) {
                    ClientHandler deleteHandler = connectedHandlerMap.get(address);
                    if (deleteHandler != null) {
                        deleteHandler.close();
                        connectedHandlerMap.remove(address);
                    }
                    connectedhandlerList.remove(handler);
                }
            }
        } else {
            log.error("no available server address");
            clearConnected();
        }
    }

    public ClientHandler chooseHandler() {
        CopyOnWriteArrayList<ClientHandler> handlers =
                (CopyOnWriteArrayList<ClientHandler>) this.connectedhandlerList.clone();
        int size = handlers.size();
        while (isRunning && size <= 0) {
            try {
                boolean available = waitingForAvailableHandler();
                if (available) {
                    handlers = (CopyOnWriteArrayList<ClientHandler>) this.connectedhandlerList.clone();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!isRunning) {
            return null;
        }
        return handlers.get(((handlerIndex.getAndIncrement() + size) % size));
    }

    public void stop() {
        isRunning = false;
        for (ClientHandler handler : connectedhandlerList) {
            handler.close();
        }
        signalAvailableHandler();
        asyncSubmitConnectedExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }

    public void reconnect(final ClientHandler handler, final SocketAddress address) {
        if (handler != null) {
            handler.close();
            connectedhandlerList.remove(handler);
            connectedHandlerMap.remove(address);
        }
        connectAsync(address);
    }

    private boolean waitingForAvailableHandler() throws InterruptedException {
        connectedLock.lock();
        try {
            return connectedCondition.await(this.connectedTimeoutMills, TimeUnit.MILLISECONDS);
        } finally {
            connectedLock.unlock();
        }
    }

    private void connectAsync(SocketAddress address) {
        asyncSubmitConnectedExecutor.submit(() -> {
            Bootstrap b = new Bootstrap();
            b.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ClientInitializer());
            connect(b, address);
        });
    }

    private void connect(Bootstrap b, SocketAddress address) {
        final ChannelFuture future = b.connect(address);
        future.channel()
                .closeFuture()
                .addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        log.info("future channel close operationComplete, address: "
                                + ((InetSocketAddress) address).getHostName());
                        future.channel()
                                .eventLoop()
                                .schedule(() -> {
                                    log.warn("connected fail to reconnected!!!");
                                    clearConnected();
                                    connect(b, address);
                                }, 3, TimeUnit.SECONDS);
                    }
                });

        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                    log.info("connect to remote server successfully, address: " + address);
                    ClientHandler handler = channelFuture.channel().pipeline().get(ClientHandler.class);
                    addHandler(handler);
                }
            }
        });
    }

    private void addHandler(ClientHandler handler) {
        connectedhandlerList.add(handler);
        SocketAddress address = handler.getAddress();
        connectedHandlerMap.put(address, handler);
        signalAvailableHandler();
    }

    private void signalAvailableHandler() {
        connectedLock.lock();
        try {
            connectedCondition.signalAll();
        } finally {
            connectedLock.unlock();
        }
    }

    private void clearConnected() {
        for (final ClientHandler handler : connectedhandlerList) {
            SocketAddress address = handler.getAddress();
            ClientHandler sendHandler = connectedHandlerMap.get(address);
            if (sendHandler != null) {
                sendHandler.close();
                connectedHandlerMap.remove(address);
            }
        }
        connectedhandlerList.clear();
    }





    public enum ConnectManagerEnum {
        RPC_CONNECT_MANAGER;

        private ConnectManager connectManager = null;

        ConnectManagerEnum() {
            connectManager = new ConnectManager();
        }

        public ConnectManager getInstance() {
            return connectManager;
        }
    }
}
