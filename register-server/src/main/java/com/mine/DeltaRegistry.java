package com.mine;

import com.mine.ServiceRegistry.RecentlyChangedServiceInstance;
import lombok.Data;

import java.util.LinkedList;

/**
 * 增量注册表
 */
@Data
public class DeltaRegistry {
    private LinkedList<RecentlyChangedServiceInstance> recentlyChangedQueue;
    private Long serviceInstanceTotalCount;

    public DeltaRegistry(LinkedList<RecentlyChangedServiceInstance> recentlyChangedQueue, Long serviceInstanceTotalCount) {
        this.recentlyChangedQueue = recentlyChangedQueue;
        this.serviceInstanceTotalCount = serviceInstanceTotalCount;
    }
}
