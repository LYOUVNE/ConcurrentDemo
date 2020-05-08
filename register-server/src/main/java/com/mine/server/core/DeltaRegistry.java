package com.mine.server.core;

import com.mine.server.core.ServiceRegistry.RecentlyChangedServiceInstance;
import lombok.Data;

import java.util.Queue;

/**
 * 增量注册表
 */
@Data
public class DeltaRegistry {
    private Queue<RecentlyChangedServiceInstance> recentlyChangedQueue;
    private Long serviceInstanceTotalCount;

    public DeltaRegistry(Queue<RecentlyChangedServiceInstance> recentlyChangedQueue, Long serviceInstanceTotalCount) {
        this.recentlyChangedQueue = recentlyChangedQueue;
        this.serviceInstanceTotalCount = serviceInstanceTotalCount;
    }
}
