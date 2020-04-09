package com.mine;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Applications {
    private Map<String, Map<String,ServiceInstance>> registry = new HashMap<>();

    public Applications() {
    }

    public Applications(Map<String, Map<String, ServiceInstance>> registry) {
        this.registry = registry;
    }
}
