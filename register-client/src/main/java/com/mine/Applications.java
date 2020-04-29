package com.mine;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class Applications {
    private Map<String, ConcurrentHashMap<String,ServiceInstance>> registry = new ConcurrentHashMap<>();

    public Applications() {
    }

    public Applications(Map<String, ConcurrentHashMap<String, ServiceInstance>> registry) {
        this.registry = registry;
    }
}
