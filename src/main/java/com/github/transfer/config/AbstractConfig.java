package com.github.transfer.config;

import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.StringUtils;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-21
 */
public abstract class AbstractConfig {

    private final AtomicInteger generator = new AtomicInteger(0);
    protected String id;
    protected String interfaceClass = null;
    protected Class<?> proxyClass = null;

    public String getId() {
        if (StringUtils.isBlank(id)) {
            id = "rapid-cfg-gen-" + generator.getAndIncrement();
        }
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setInterface(String interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public String getInterface() {
        return this.interfaceClass;
    }
}
