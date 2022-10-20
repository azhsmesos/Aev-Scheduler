package com.github.transfer.codec;

import java.io.Serializable;
import lombok.Data;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-19
 */
@Data
public class Request implements Serializable {

    private String requestID;

    private String className;

    private String methodName;

    private Class<?>[] parameterTypes;

    private Object[] parameters;
}
