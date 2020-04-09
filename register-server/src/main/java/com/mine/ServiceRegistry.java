package com.mine;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * 注册表
 * @author zhonghuashishan
 *
 */
public class ServiceRegistry {

	public static final Long RECENTLY_CHANGED_ITEM_CHECK_INTERVAL = 3000L;
	public static final Long RECENTLY_CHANGED_ITEM_EXPIRED = 3 * 60 * 1000L;

	/**
	 * 注册表是一个单例
	 */
	private static ServiceRegistry instance = new ServiceRegistry();
	
	private LinkedList<RecentlyChangedServiceInstance> recentlyChangedQueue = new LinkedList<>();
	
	/**
	 * 核心的内存数据结构：注册表
	 * 
	 * Map：key是服务名称，value是这个服务的所有的服务实例
	 * Map<String, ServiceInstance>：key是服务实例id，value是服务实例的信息
	 * 
	 */
	private Map<String, Map<String, ServiceInstance>> registry = 
			new HashMap<String, Map<String, ServiceInstance>>();

	private ServiceRegistry() {
		RecentlyChangedQueueMonitor recentlyChangedQueueMonitor = new RecentlyChangedQueueMonitor();
		recentlyChangedQueueMonitor.setDaemon(true);
		recentlyChangedQueueMonitor.start();
	}

	/**
	 * 服务注册
	 * @param serviceInstance 服务实例
	 */
	public synchronized void register(ServiceInstance serviceInstance) {
		// 将服务实例放入最近变更的队列中
		RecentlyChangedServiceInstance recentlyChangedItem = new RecentlyChangedServiceInstance(
				serviceInstance,
				System.currentTimeMillis(),
				ServiceInstanceOperation.REGISTER);
		recentlyChangedQueue.offer(recentlyChangedItem);

		// 将服务实例放入注册表中
		Map<String, ServiceInstance> serviceInstanceMap = 
				registry.get(serviceInstance.getServiceName());
		
		if(serviceInstanceMap == null) {
			serviceInstanceMap = new HashMap<String, ServiceInstance>();
			registry.put(serviceInstance.getServiceName(), serviceInstanceMap);
		}
		
		serviceInstanceMap.put(serviceInstance.getServiceInstanceId(), 
				serviceInstance);
		
		System.out.println("服务实例，完成注册......【" + serviceInstance + "】");
		System.out.println("注册表：" + registry); 
	}
	
	/**
	 * 获取服务实例
	 * @param serviceName 服务名称
	 * @param serviceInstanceId 服务实例id
	 * @return 服务实例
	 */
	public synchronized ServiceInstance getServiceInstance(String serviceName,
			String serviceInstanceId) {
		Map<String, ServiceInstance> serviceInstanceMap = registry.get(serviceName);
		return serviceInstanceMap.get(serviceInstanceId);
	}
	
	/**
	 * 获取整个注册表
	 * @return
	 */
	public synchronized Map<String, Map<String, ServiceInstance>> getRegistry() {
		return registry;
	}

	/**
	 * 获取最近有变化的注册表
	 * @return
	 */
	public synchronized DeltaRegistry getDeltaRegistry() {
		Long totalCount = 0L;
		for(Map<String, ServiceInstance> serviceInstanceMap : registry.values()) {
			totalCount += serviceInstanceMap.size();
		}

		DeltaRegistry deltaRegistry = new DeltaRegistry(
				recentlyChangedQueue, totalCount);

		return deltaRegistry;
	}
	
	/**
	 * 从注册表删除一个服务实例
	 * @param serviceName
	 * @param serviceInstanceId
	 */
	public synchronized void remove(String serviceName, String serviceInstanceId) {
		System.out.println("服务实例从注册表中摘除【" + serviceName + ", " + serviceInstanceId + "】");

		// 获取服务实例
		Map<String, ServiceInstance> serviceInstanceMap = registry.get(serviceName);
		ServiceInstance serviceInstance = serviceInstanceMap.get(serviceInstanceId);

		// 将服务实例放入最近变更的队列中
		RecentlyChangedServiceInstance recentlyChangedItem = new RecentlyChangedServiceInstance(
				serviceInstance,
				System.currentTimeMillis(),
				ServiceInstanceOperation.REMOVE);
		recentlyChangedQueue.offer(recentlyChangedItem);

		// 从服务注册表删除服务实例
		serviceInstanceMap.remove(serviceInstanceId);
	}

	/**
	 * 获取服务注册表的单例实例
	 * @return
	 */
	public static ServiceRegistry getInstance() {
		return instance;
	}

	/**
	 * 最近变化的服务实例
	 */
	class RecentlyChangedServiceInstance {
		/**
		 * 服务实例
		 */
		ServiceInstance serviceInstance;
		/**
		 * 发生变更的时间戳
		 */
		long changedTimestamp;
		/**
		 * 变更操作
		 */
		String serviceInstanceOperation;

		public RecentlyChangedServiceInstance(ServiceInstance serviceInstance, long changedTimestamp, String serviceInstanceOperation) {
			this.serviceInstance = serviceInstance;
			this.changedTimestamp = changedTimestamp;
			this.serviceInstanceOperation = serviceInstanceOperation;
		}
	}

	/**
	 * 服务实例操作
	 */
	class ServiceInstanceOperation {
		/**
		 * 注册
		 */
		public static final String REGISTER = "register";
		/**
		 * 删除
		 */
		public static final String REMOVE = "remove";
	}

	/**
	 * 最近变更队列的监控线程
	 */
	class RecentlyChangedQueueMonitor extends Thread {
		@Override
		public void run() {
			while (true) {
				try {
					synchronized (instance) {
						RecentlyChangedServiceInstance recentlyChangedItem = null;
						long currentTimestamp = System.currentTimeMillis();

						while ((recentlyChangedItem = recentlyChangedQueue.peek()) != null) {
							// 判断如果一个服务实例变更信息已经在队列里存在超过3分钟了
							// 就从队列中移除
							if (currentTimestamp  - recentlyChangedItem.changedTimestamp > RECENTLY_CHANGED_ITEM_EXPIRED) {
								recentlyChangedQueue.pop();
							}
						}
					}

					Thread.sleep(RECENTLY_CHANGED_ITEM_CHECK_INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
