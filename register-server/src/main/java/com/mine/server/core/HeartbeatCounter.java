package com.mine.server.core;

import java.util.concurrent.atomic.AtomicLong;

public class HeartbeatCounter {
    /**
     * 单例实例
     */
    private static HeartbeatCounter instance = new HeartbeatCounter();

    /**
     * 最近一分钟的心跳次数
     */
//    private LongAdder lastMinuteHeartbeat = new LongAdder();
    private AtomicLong lastMinuteHeartbeatRate = new AtomicLong(0L);

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
        // 这个synchronized上锁，性能其实是很差的
        // 因为可能会有很多的线程，不断的接受到心跳的请求，就会来增加心跳次数
        // 多线程卡在这里，一个一个排队
        // 依次上锁，累加1，再次释放锁，会有一个问题

        // 如果你的服务实例很多的话，1万个服务实例，每秒可能都有很多个请求过来更新心跳
        // 如果在这里加了synchronized的话，会影响并发的性能
        // 换成了AtomicLong原子类之后，不加锁，无锁化，CAS操作，保证原子性，还可以多线程并发
//        lastMinuteHeartbeat.increment();
        lastMinuteHeartbeatRate.incrementAndGet();
    }


    /**
     * 获取最近一分钟的心跳次数
     */
    public /**synchronized*/ long get(){
        return lastMinuteHeartbeatRate.get();
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
                            while (true) {
                                long expectedValue = lastMinuteHeartbeatRate.get();
                                if (lastMinuteHeartbeatRate.compareAndSet(expectedValue, 0L)) {
                                    break;
                                }
                            }
                        }
                        lastMinuteTimeStamp = System.currentTimeMillis();
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
