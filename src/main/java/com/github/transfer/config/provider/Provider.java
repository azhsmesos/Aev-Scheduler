package com.github.transfer.config.provider;

import com.github.transfer.config.AbstractConfig;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-21
 */
public class Provider extends AbstractConfig {

    protected Object ref;


    public Object getRef() {
        return this.ref;
    }

    public void setRef(Object ref) {
        this.ref = ref;
    }
}
