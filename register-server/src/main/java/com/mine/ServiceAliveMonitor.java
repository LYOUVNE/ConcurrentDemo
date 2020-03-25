package com.mine;

import java.util.Map;

/**
 * 微服务存活状态监控组件
 * @author zhonghuashishan
 *
 */
public class ServiceAliveMonitor {
	
	/**
	 * 检查服务实例是否存活的间隔
	 */
	private static final Long CHECK_ALIVE_INTERVAL = 60 * 1000L;

	/**
	 * 负责监控微服务存活状态的后台线程
	 */
	private Daemon daemon;

	public ServiceAliveMonitor() {
		daemon = new Daemon();
		daemon.setDaemon(true);
		daemon.setName("ServiceAliveMonitor");
	}
	
	/**
	 * 启动后台线程
	 */
	public void start() {
		daemon.start();
	}

	/**
	 * 负责监控微服务存活状态的后台线程
	 * @author zhonghuashishan
	 *
	 */
	private class Daemon extends Thread {
		
		private ServiceRegistry registry = ServiceRegistry.getInstance();
		
		@Override
		public void run() {
			Map<String, Map<String, ServiceInstance>> registryMap = null;
			
			while(true) {
				try {
					// 可以判断一下是否要开启自我保护机制
					SelfProtectionPolicy selfProtectionPolicy = SelfProtectionPolicy.getInstance();
					if (selfProtectionPolicy.isEnable()) {
						Thread.sleep(CHECK_ALIVE_INTERVAL);
						continue;
					}

					registryMap = registry.getRegistry();
					
					for(String serviceName : registryMap.keySet()) {
						Map<String, ServiceInstance> serviceInstanceMap = 
								registryMap.get(serviceName);
						
						for(ServiceInstance serviceInstance : serviceInstanceMap.values()) {
							// 说明服务实例距离上一次发送心跳已经超过90秒了
							// 认为这个服务就死了
							// 从注册表中摘除这个服务实例
							if(!serviceInstance.isAlive()) {
								registry.remove(serviceName, serviceInstance.getServiceInstanceId());

								// 更新自我保护机制的阈值
								synchronized (SelfProtectionPolicy.class) {
									selfProtectionPolicy.setExpectedHeartbeatRate(selfProtectionPolicy.getExpectedHeartbeatRate() - 2);
									selfProtectionPolicy.setExpectedHeartbeatThreshold((long) (selfProtectionPolicy.getExpectedHeartbeatThreshold() * 0.85));
								}
							}
						}
					}
					
					Thread.sleep(CHECK_ALIVE_INTERVAL);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
}
