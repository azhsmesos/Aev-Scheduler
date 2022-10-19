package com.github.transfer.client;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-19
 */
public class Client {

    private String serverAddress;

    private long timeout;

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
}
