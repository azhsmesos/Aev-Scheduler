package com.github.transfer.consumer;

import com.github.transfer.client.Client;
import com.github.transfer.client.RpcFuture;
import com.github.transfer.client.proxy.AsyncProxy;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-23
 */
public class ConsumerStarter {


    public static void sync() {
        Client client = new Client();
        client.initClient("127.0.0.1:8765", 3000);
        HelloService service = client.invokeSync(HelloService.class);
        String result = service.hello("zhangsan");
        System.out.println(result);
    }

    public static void async() throws Exception {
        Client client = new Client();
        client.initClient("127.0.0.1:8765", 3000);
        AsyncProxy proxy = client.invokeAsync(HelloService.class);
        RpcFuture future = proxy.call("zzh", "li4");
        RpcFuture future2 = proxy.call("azh", new User("zzh"));
        Object res = future.get();
        Object res2 = future2.get();
        System.out.println("res: " + res);
        System.out.println("res2: " + res2);
    }

    public static void main(String[] args) {
        sync();
    }
}
