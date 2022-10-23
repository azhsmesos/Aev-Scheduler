package com.github.transfer.provider;

import com.github.transfer.config.provider.Provider;
import com.github.transfer.config.provider.ServerConfig;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-23
 */
public class ProviderStarter {

    public static void main(String[] args) {
        new Thread(() -> {
            try {
                Provider provider = new Provider();
                provider.setInterface("com/github/transfer/consumer/HelloService");

                HelloServiceImpl impl = HelloServiceImpl.class.newInstance();
                provider.setRef(impl);

                List<Provider> providers = new ArrayList<>();
                providers.add(provider);

                ServerConfig config = new ServerConfig(providers);
                config.setPort(8765);
                config.exporter();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
