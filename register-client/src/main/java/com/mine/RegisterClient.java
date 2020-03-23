package com.mine;

import java.util.UUID;

/**
 * 在服务上被创建和启动，负责跟register-server进行通信
 *
 */
public class RegisterClient {

	public static final String SERVICE_NAME = "inventory-service";
	public static final String IP = "192.168.31.207";
	public static final String HOSTNAME = "inventory01";
	public static final int PORT = 9000;

	/**
	 * 服务实例id
	 */
	private String serviceInstanceId;

	/**
	 * http通信组件
	 */
	private HttpSender httpSender;
	/**
	 * 心跳线程
	 */
	private HeartbeatWorker heartbeatWorker;
	/**
	 * 服务实例是否在运行
	 */
	private volatile Boolean isRunning;
	
	public RegisterClient() {
		this.serviceInstanceId = UUID.randomUUID().toString().replace("-", "");
		this.httpSender = new HttpSender();
		this.heartbeatWorker = new HeartbeatWorker();
		this.isRunning = true;
	}
	
	public void start() {
		// 一旦启动了这个组件之后，他就负责在服务上干两个事情
		// 第一个事情，就是开启一个线程向register-server去发送请求，注册这个服务
		// 第二个事情，就是在注册成功之后，就会开启另外一个线程去发送心跳
		
		// 我们来简化一下这个模型
		// 我们在register-client这块就开启一个线程
		// 这个线程刚启动的时候，第一个事情就是完成注册
		// 如果注册完成了之后，他就会进入一个while true死循环
		// 每隔30秒就发送一个请求去进行心跳

		try {
			RegisterClientWorker registerClientWorker = new RegisterClientWorker(serviceInstanceId);
			registerClientWorker.start();
			registerClientWorker.join();

			heartbeatWorker.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void shutDown() {
		this.isRunning = false;
		this.heartbeatWorker.interrupt();
	}
	
	/**
	 * 负责向register-server发起注册申请的线程
	 * @author zhonghuashishan
	 *
	 */
	private class RegisterClientWorker extends Thread {
		/**
		 * 服务实例id
		 */
		private String serviceInstanceId;
		
		public RegisterClientWorker(String serviceInstanceId) {
			this.serviceInstanceId = serviceInstanceId;
		}
		
		@Override
		public void run() {
			// 应该是获取当前机器的信息
			// 包括当前机器的ip地址、hostname，以及你配置这个服务监听的端口号
			// 从配置文件里可以拿到
			RegisterRequest registerRequest = new RegisterRequest();
			registerRequest.setServiceName(SERVICE_NAME);
			registerRequest.setIp(IP);
			registerRequest.setHostname(HOSTNAME);
			registerRequest.setPort(PORT);
			registerRequest.setServiceInstanceId(serviceInstanceId);

			RegisterResponse registerResponse = httpSender.register(registerRequest);

			System.out.println("服务注册的结果是：" + registerResponse.getStatus() + "......");
	 	}
		
	}

	private class HeartbeatWorker extends Thread {
		@Override
		public void run() {
			HeartbeatRequest heartbeatRequest = new HeartbeatRequest();
			heartbeatRequest.setServiceName(SERVICE_NAME);
			heartbeatRequest.setServiceInstanceId(serviceInstanceId);
			HeartbeatResponse heartbeatResponse = null;

			while(isRunning) {
				try {
					heartbeatResponse = httpSender.heartbeat(heartbeatRequest);
					System.out.println("心跳的结果为：" + heartbeatResponse.getStatus() + "......");
					Thread.sleep(30 * 1000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
