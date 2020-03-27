package com.mine;

import java.util.concurrent.atomic.AtomicLong;

public class HeartbeatCounter {
    /**
     * 单例实例
     */
    private static HeartbeatCounter instance = new HeartbeatCounter();

    /**
     * 最近一分钟的心跳次数
     */
    private AtomicLong lastMinuteHeartbeat = new AtomicLong(0L);

    /**
     * 最近一分钟的时间戳
     */
    private long lastMinuteTimeStamp = System.currentTimeMillis();

    public HeartbeatCounter(){
        Daemon daemon = new Daemon();
        daemon.setDaemon(true);
        daemon.start();
    }

    /**
     * 获取单例实例
     * @return
     */
    public static HeartbeatCounter getInstance(){
        return instance;
    }

    /**
     * 增加一次最近一分钟的心跳次数
     */
    public /**synchronized*/ void increment(){
        lastMinuteHeartbeat.incrementAndGet();
    }

    /**
     * 获取最近一分钟的心跳次数
     */
    public /**synchronized*/ long get(){
        return lastMinuteHeartbeat.get();
    }

    /**
     * 后台线程定时清空上一分钟的心跳次数，否则不发心跳就不清空
     */
    private class Daemon extends Thread{
        @Override
        public void run() {
            while (true) {
                try {
                    synchronized (HeartbeatCounter.class) {
                        if (System.currentTimeMillis() - lastMinuteTimeStamp > 60 * 1000) {
                            lastMinuteTimeStamp = System.currentTimeMillis();
                            lastMinuteHeartbeat = new AtomicLong(0L);
                        }
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
