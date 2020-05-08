package com.mine.server;

import lombok.Data;

/**
 * 注册响应
 * @author zhonghuashishan
 *
 */
@Data
public class RegisterResponse {
	
	public static final String SUCCESS = "success";
	public static final String FAILURE = "failure";

	/**
	 * 注册响应状态：SUCCESS、FAILURE
	 */
	private String status;
}
