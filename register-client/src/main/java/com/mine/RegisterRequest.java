package com.mine;

import lombok.Data;

/**
 * 注册请求
 * @author zhonghuashishan
 *
 */
@Data
public class RegisterRequest {

	/**
	 * 服务名称
	 */
	private String serviceName;
	/**
	 * 服务所在机器的ip地址
	 */
	private String ip;
	/**
	 * 服务所在机器的主机名
	 */
	private String hostname;
	/**
	 * 服务监听着哪个端口号
	 */
	private int port;
	/**
	 * 服务实例
	 */
	private String serviceInstanceId;
}
