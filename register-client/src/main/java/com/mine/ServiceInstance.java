package com.mine;

import lombok.Data;

/**
 * 代表了一个服务实例
 * 里面包含了一个服务实例的所有信息
 * 比如说服务名称、ip地址、hostname、端口号、服务实例id
 */
@Data
public class ServiceInstance {
	
	/**
	 * 判断一个服务实例不再存活的周期
	 */
	private static final Long NOT_ALIVE_PERIOD = 90 * 1000L;

	/**
	 * 服务名称
	 */
	private String serviceName;
	/**
	 * ip地址
	 */
	private String ip;
	/**
	 * 主机名
	 */
	private String hostname;
	/**
	 * 端口号
	 */
	private int port;
	/**
	 * 服务实例id
	 */
	private String serviceInstanceId;
}
