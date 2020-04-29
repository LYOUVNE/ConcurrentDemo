package com.mine;

import com.mine.CachedServiceRegistry.RecentlyChangedServiceInstance;
import lombok.Data;

import java.util.LinkedList;
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
