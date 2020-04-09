package com.mine;

import com.mine.CachedServiceRegistry.RecentlyChangedServiceInstance;
import lombok.Data;

import java.util.LinkedList;

@Data
public class DeltaRegistry {
    private LinkedList<RecentlyChangedServiceInstance> recentlyChangedQueue;
    private Long serviceInstanceTotalCount;

    public DeltaRegistry(LinkedList<RecentlyChangedServiceInstance> recentlyChangedQueue, Long serviceInstanceTotalCount) {
        this.recentlyChangedQueue = recentlyChangedQueue;
        this.serviceInstanceTotalCount = serviceInstanceTotalCount;
    }
}
