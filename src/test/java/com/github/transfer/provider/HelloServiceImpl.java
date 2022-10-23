package com.github.transfer.provider;

import com.github.transfer.consumer.HelloService;
import com.github.transfer.consumer.User;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-23
 */
public class HelloServiceImpl implements HelloService {
    @Override
    public String hello(String name) {
        return "hello " + name;
    }

    @Override
    public String hello(User user) {
        return "hello " + user.getName();
    }
}
