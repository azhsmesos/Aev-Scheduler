package com.github.transfer.consumer;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-23
 */
public interface HelloService {


    String hello(String name);

    String hello(User user);
}
