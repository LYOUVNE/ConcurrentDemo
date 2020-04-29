package com.mine;

import com.mine.ServiceRegistry.RecentlyChangedServiceInstance;
import lombok.Data;

import java.util.LinkedList;
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
