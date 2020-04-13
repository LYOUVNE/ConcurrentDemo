package com.mine;

import java.util.ArrayList;
import java.util.List;
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

					// 定义要删除的服务实例的集合
					List<ServiceInstance> removingServiceInstance = new ArrayList<ServiceInstance>();

					// 开始读服务注册表的数据，这个过程中，别人可以读，但是不可以写
					try {
						// 对整个服务注册表，加读锁
						registry.readLock();

						registryMap = registry.getRegistry();
						for(String serviceName : registryMap.keySet()) {
							Map<String, ServiceInstance> serviceInstanceMap =
									registryMap.get(serviceName);

							for (ServiceInstance serviceInstance : serviceInstanceMap.values()) {
								// 说明服务实例距离上一次发送心跳已经超过90秒了
								// 认为这个服务就死了
								// 从注册表中摘除这个服务实例
								if (!serviceInstance.isAlive()) {
									removingServiceInstance.add(serviceInstance);
								}
							}
						}
					} finally {
						registry.readUnLock();
					}

					// 将所有的要删除的服务实例，从服务注册表删除
					for(ServiceInstance serviceInstance : removingServiceInstance) {
						registry.remove(serviceInstance.getServiceName(), serviceInstance.getServiceInstanceId());

						// 更新自我保护机制的阈值
						synchronized (SelfProtectionPolicy.class) {
							selfProtectionPolicy.setExpectedHeartbeatRate(selfProtectionPolicy.getExpectedHeartbeatRate() - 2);
							selfProtectionPolicy.setExpectedHeartbeatThreshold((long) (selfProtectionPolicy.getExpectedHeartbeatThreshold() * 0.85));
						}
					}

					if (removingServiceInstance.size() == 0) {
						// 过期注册表缓存
						ServiceRegistryCache.getInstance().invalidate();
					}

					Thread.sleep(CHECK_ALIVE_INTERVAL);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
