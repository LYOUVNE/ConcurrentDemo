package com.mine.server;

import lombok.Data;

/**
 * 心跳请求
 *
 */
@Data
public class HeartbeatRequest {

	/**
	 * 服务名称
	 */
	private String serviceName;
	/**
	 * 服务实例id
	 */
	private String serviceInstanceId;
}
