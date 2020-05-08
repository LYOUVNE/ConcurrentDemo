package com.mine.server;

import com.mine.server.CachedServiceRegistry.RecentlyChangedServiceInstance;
import lombok.Data;

import java.util.Queue;

@Data
public class DeltaRegistry {
    private Queue<RecentlyChangedServiceInstance> recentlyChangedQueue;
    private Long serviceInstanceTotalCount;

    public DeltaRegistry(Queue<RecentlyChangedServiceInstance> recentlyChangedQueue, Long serviceInstanceTotalCount) {
        this.recentlyChangedQueue = recentlyChangedQueue;
        this.serviceInstanceTotalCount = serviceInstanceTotalCount;
    }
}
