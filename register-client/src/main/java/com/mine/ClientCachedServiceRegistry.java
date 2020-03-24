package com.mine;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务注册中心的客户端缓存的一个服务注册表
 */
public class ClientCachedServiceRegistry {
    /**
     * 服务注册表拉取间隔时间
     */
    private static final Long SERVICE_REGISTRY_FETCH_INTERVAL = 30 * 1000L;

    private Map<String,Map<String,ServiceInstance>> registry = new HashMap<String, Map<String, ServiceInstance>>();

    /**
     * 负责定时拉取注册表到客户端进行缓存的后台线程
     */
    private Deamon deamon;

    /**
     * RegisterClient
     */
    private RegisterClient registerClient;

    /**
     * http通信组件
     */
    private HttpSender httpSender;

    public ClientCachedServiceRegistry(RegisterClient registerClient,HttpSender httpSender) {
        this.deamon = new Deamon();
        this.registerClient = registerClient;
        this.httpSender = httpSender;
    }

    /**
     * 初始化
     */
    public void initialize(){
        this.deamon.start();
    }

    /**
     * 销毁这个组件
     */
    public void destory(){
        this.deamon.interrupt();
    }

    /**
     * 负责定时拉取注册表到本地来进行缓存
     * @author zhonghuashishan
     *
     */
    private class Deamon extends Thread {
        @Override
        public void run() {
            while (registerClient.isRunning()) {
                try {
                    registry = httpSender.fetchServiceRegistry();
                    Thread.sleep(SERVICE_REGISTRY_FETCH_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取服务注册表
     * @return
     */
    public Map<String,Map<String,ServiceInstance>> getRegistry(){
        return registry;
    }
}
