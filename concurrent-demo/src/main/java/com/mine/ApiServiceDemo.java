package com.mine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;

public class ApiServiceDemo {

    public Map<String, Object> queryOrders() throws Exception {
        List<Object> results = new ArrayList();
        Map<String, Object> map = new ConcurrentHashMap<>();

        CyclicBarrier cyclicBarrier = new CyclicBarrier(3, new Runnable() {
            @Override
            public void run() {
                map.put("price", results.get(0));
                map.put("order", results.get(1));
                map.put("stats", results.get(2));
            }
        });

        new Thread() {
            @Override
            public void run() {
                try {
                    System.out.println("请求价格服务......");
                    Thread.sleep(1000);
                    results.add(new Object());
                    cyclicBarrier.await();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        new Thread() {
            @Override
            public void run() {
                try {
                    System.out.println("请求订单服务......");
                    Thread.sleep(1000);
                    results.add(new Object());
                    cyclicBarrier.await();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        new Thread() {
            @Override
            public void run() {
                try {
                    System.out.println("请求订单统计服务......");
                    Thread.sleep(1000);
                    results.add(new Object());
                    cyclicBarrier.await();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        while (map.size() < 3) {
            Thread.sleep(100);
        }

        return map;
    }
}
