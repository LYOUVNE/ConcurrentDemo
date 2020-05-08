package com.mine.server.datanode;

/**
 * DataNode启动类
 */
public class DataNode {
    /**
     * 是否还在运行
     */
    private volatile boolean shouldRun;

    /**
     * 负责跟一组NameNode通信的组件
     */
    private NameNodeOfferService offerService;

    /**
     * 初始化DataNode
     */
    private void initialize() {
        this.shouldRun = true;
        this.offerService = new NameNodeOfferService();
        this.offerService.start();
    }

    /**
     * 运行DataNode
     */
    private void run() {
        try {
            while (shouldRun) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        DataNode dataNode = new DataNode();
        dataNode.initialize();
        dataNode.run();
    }

}
