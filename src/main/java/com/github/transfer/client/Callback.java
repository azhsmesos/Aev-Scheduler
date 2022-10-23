package com.github.transfer.client;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-22
 */
public interface Callback {

    void success(Object result);

    void failure(Throwable throwable);
}
