package com.github.transfer.config.provider;

import com.github.transfer.server.Server;
import java.util.List;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-23
 */
public class ServerConfig {

    private final String host = "127.0.0.1";
    protected int port;

    private List<Provider> providers;

    private Server server = null;

    public ServerConfig(List<Provider> providers) {
        this.providers = providers;
    }

    public void exporter() throws InterruptedException {
        if (server == null) {
            server = new Server(host + ":" + port);
        }
        for (Provider provider : providers) {
            server.registerProcessor(provider);
        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<Provider> getProviders() {
        return providers;
    }

    public void setProviders(List<Provider> providers) {
        this.providers = providers;
    }
}
