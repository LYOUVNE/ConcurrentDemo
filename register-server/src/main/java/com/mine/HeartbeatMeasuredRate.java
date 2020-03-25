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

    public HeartbeatMeasuredRate(){
        Daemon daemon = new Daemon();
        daemon.setDaemon(true);
        daemon.start();
    }

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
        lastMinuteHeartbeat++;
    }

    /**
     * 获取最近一分钟的心跳次数
     */
    public synchronized long get(){
        return lastMinuteHeartbeat;
    }

    /**
     * 后台线程定时清空上一分钟的心跳次数，否则不发心跳就不清空
     */
    private class Daemon extends Thread{
        @Override
        public void run() {
            while (true) {
                try {
                    synchronized (HeartbeatMeasuredRate.class) {
                        if (System.currentTimeMillis() - lastMinuteTimeStamp > 60 * 1000) {
                            lastMinuteTimeStamp = System.currentTimeMillis();
                            lastMinuteHeartbeat = 0L;
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
