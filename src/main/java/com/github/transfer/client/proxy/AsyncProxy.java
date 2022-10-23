package com.github.transfer.client.proxy;

import com.github.transfer.client.RpcFuture;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-23
 */
public interface AsyncProxy {

    RpcFuture call(String methodName, Object... args);
}
