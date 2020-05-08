package com.mine.server.web;

import lombok.Data;

@Data
public abstract class AbstractRequest {
    public static final Integer REGISTER_REQUEST = 1;
    public static final Integer CANCEL_REQUEST = 2;
    public static final Integer HEARTBEAT_REQUEST = 3;

    /**
     * 服务名称
     */
    protected String serviceName;
    /**
     * 服务实例id
     */
    protected String serviceInstanceId;
    /**
     * 请求类型
     */
    protected Integer type;
}
