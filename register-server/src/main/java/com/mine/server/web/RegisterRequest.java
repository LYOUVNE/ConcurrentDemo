package com.mine.server.web;

import lombok.Data;

/**
 * 注册请求
 * @author zhonghuashishan
 *
 */
@Data
public class RegisterRequest extends AbstractRequest{
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
}
