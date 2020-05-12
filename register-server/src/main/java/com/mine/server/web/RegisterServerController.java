package com.mine.server.web;

import com.mine.server.cluster.PeersReplicateBatch;
import com.mine.server.cluster.PeersReplicator;
import com.mine.server.core.*;

/**
 * 这个controller是负责接收register-client发送过来的请求的
 * 在Spring Cloud Eureka中用的组件是jersey，百度一下jersey是什么东西
 * 在国外很常用jersey，restful框架，可以接受http请求
 * 
 * @author zhonghuashishan
 *
 */
public class RegisterServerController {

	private ServiceRegistry registry = ServiceRegistry.getInstance();

	/**
	 * 服务注册表的缓存
	 */
	private ServiceRegistryCache registryCache = ServiceRegistryCache.getInstance();

	/**
	 * 集群同步组件
	 */
	private PeersReplicator peersReplicator = PeersReplicator.getInstance();
	
	/**
	 * 服务注册
	 * @param registerRequest 注册请求
	 * @return 注册响应
	 */
	public RegisterResponse register(RegisterRequest registerRequest) {
		RegisterResponse registerResponse = new RegisterResponse();
		
		try {
			ServiceInstance serviceInstance = new ServiceInstance();
			serviceInstance.setHostname(registerRequest.getHostname()); 
			serviceInstance.setIp(registerRequest.getIp()); 
			serviceInstance.setPort(registerRequest.getPort()); 
			serviceInstance.setServiceInstanceId(registerRequest.getServiceInstanceId()); 
			serviceInstance.setServiceName(registerRequest.getServiceName());

			registry.register(serviceInstance);

			// 更新自我保护机制的阈值
			synchronized(SelfProtectionPolicy.class) {
				SelfProtectionPolicy selfProtectionPolicy = SelfProtectionPolicy.getInstance();
				selfProtectionPolicy.setExpectedHeartbeatRate(
						selfProtectionPolicy.getExpectedHeartbeatRate() + 2);
				selfProtectionPolicy.setExpectedHeartbeatThreshold(
						(long) (selfProtectionPolicy.getExpectedHeartbeatRate() * 0.85));
			}

			// 过期掉注册表缓存
			registryCache.invalidate();

			// 进行集群同步
			peersReplicator.replicateRegister(registerRequest);

			registerResponse.setStatus(RegisterResponse.SUCCESS);
		} catch (Exception e) {
			e.printStackTrace(); 
			registerResponse.setStatus(RegisterResponse.FAILURE);  
		}
		
		return registerResponse;
	}
	
	/**
	 * 发送心跳
	 * @param heartbeatRequest 心跳请求
	 * @return 心跳响应
	 */
	public HeartbeatResponse heartbeat(HeartbeatRequest heartbeatRequest) { 
		HeartbeatResponse heartbeatResponse = new HeartbeatResponse();
		
		try {
			// 对服务实例进行续约
			ServiceInstance serviceInstance = registry.getServiceInstance(
					heartbeatRequest.getServiceName(), heartbeatRequest.getServiceInstanceId());
			serviceInstance.renew();

			// 记录一下每分钟的心跳的次数
			HeartbeatCounter heartbeatCounter = HeartbeatCounter.getInstance();
			heartbeatCounter.increment();

			// 过期掉注册表缓存
			registryCache.invalidate();

			// 进行集群同步
			peersReplicator.replicateHeartbeat(heartbeatRequest);
			
			heartbeatResponse.setStatus(HeartbeatResponse.SUCCESS); 
		} catch (Exception e) {
			e.printStackTrace(); 
			heartbeatResponse.setStatus(HeartbeatResponse.FAILURE); 
		}
		
		return heartbeatResponse;
	}

	/**
	 * 服务下线
	 */
	public void cancel(CancelRequest cancelRequest) {
		registry.remove(cancelRequest.getServiceName(), cancelRequest.getServiceInstanceId());

		// 更新自我保护机制的阈值
		synchronized(SelfProtectionPolicy.class) {
			SelfProtectionPolicy selfProtectionPolicy = SelfProtectionPolicy.getInstance();
			selfProtectionPolicy.setExpectedHeartbeatRate(
					selfProtectionPolicy.getExpectedHeartbeatRate() - 2);
			selfProtectionPolicy.setExpectedHeartbeatThreshold(
					(long)(selfProtectionPolicy.getExpectedHeartbeatRate() * 0.85));
		}

		// 过期掉注册表缓存
		registryCache.invalidate();

		// 进行集群同步
		peersReplicator.replicateCancel(cancelRequest);
	}

	/**
	 * 同步batch数据
	 * @param batch
	 */
	public void replicateBatch(PeersReplicateBatch batch) {
		for(AbstractRequest request : batch.getRequests()) {
			if(request.getType().equals(AbstractRequest.REGISTER_REQUEST)) {
				register((RegisterRequest) request);
			} else if(request.getType().equals(AbstractRequest.CANCEL_REQUEST)) {
				cancel((CancelRequest) request);
			} else if(request.getType().equals(AbstractRequest.HEARTBEAT_REQUEST)) {
				heartbeat((HeartbeatRequest) request);
			}
		}
	}

	/**
	 * 拉取全量服务注册表
	 * @return
	 */
	public Applications fetchFullRegistry() {
		return (Applications) registryCache.get(ServiceRegistryCache.CacheKey.FULL_SERVICE_REGISTRY);
	}

	/**
	 * 拉取全增量服务注册表
	 * @return
	 */
	public DeltaRegistry fetchDeltaRegistry() {
		return (DeltaRegistry) registryCache.get(ServiceRegistryCache.CacheKey.DELTA_SERVICE_REGISTRY);
	}
}
