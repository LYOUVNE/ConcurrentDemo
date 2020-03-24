package com.mine;

public class HeartbeatMeasuredRate {
    /**
     * 单例实例
     */
    private static HeartbeatMeasuredRate instance = new HeartbeatMeasuredRate();

    /**
     * 最近一分钟的心跳次数
     */
    private long lastMinuteHeartbeat = 0L;

    /**
     * 最近一分钟的时间戳
     */
    private long lastMinuteTimeStamp = System.currentTimeMillis();

    /**
     * 获取单例实例
     * @return
     */
    public static HeartbeatMeasuredRate getInstance(){
        return instance;
    }

    /**
     * 增加一次最近一分钟的心跳次数
     */
    public synchronized void increment(){
        if (System.currentTimeMillis() - lastMinuteTimeStamp > 60 * 1000) {
            this.lastMinuteTimeStamp = System.currentTimeMillis();
            lastMinuteHeartbeat = 0L;
        }
        lastMinuteHeartbeat++;
    }

    /**
     * 获取最近一分钟的心跳次数
     */
    public synchronized long get(){
        return lastMinuteHeartbeat;
    }
}
