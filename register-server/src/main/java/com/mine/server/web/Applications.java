package com.mine.server.web;

import com.mine.server.core.ServiceInstance;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class Applications {
    private Map<String, ConcurrentHashMap<String, ServiceInstance>> registry = new ConcurrentHashMap<>();

    public Applications() {
    }

    public Applications(Map<String, ConcurrentHashMap<String, ServiceInstance>> registry) {
        this.registry = registry;
    }
}
