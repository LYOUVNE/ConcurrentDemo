package com.mine;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * 服务注册中心的客户端缓存的一个服务注册表
 */
public class CachedServiceRegistry {
    /**
     * 服务注册表拉取间隔时间
     */
    private static final Long SERVICE_REGISTRY_FETCH_INTERVAL = 30 * 1000L;

//    private AtomicReference<Applications> applications = new AtomicReference<>(new Applications());
    private AtomicStampedReference<Applications> applications = new AtomicStampedReference<>(new Applications(), 0);

    /**
     * 负责定时拉取注册表到客户端进行缓存的后台线程
     */
    private FetchDeltaRegistryWorker fetchDeltaRegistryWorker;

    /**
     * RegisterClient
     */
    private RegisterClient registerClient;

    /**
     * http通信组件
     */
    private HttpSender httpSender;

    /**
     * 代表了当前的本地缓存的服务注册表的一个版本号
     */
    private AtomicLong applicationsVersion = new AtomicLong(0L);

    public CachedServiceRegistry(RegisterClient registerClient, HttpSender httpSender) {
        this.fetchDeltaRegistryWorker = new FetchDeltaRegistryWorker();
        this.registerClient = registerClient;
        this.httpSender = httpSender;
    }

    /**
     * 初始化
     */
    public void initialize(){
        FetchFullRegistryWorker fetchFullRegistryWorker = new FetchFullRegistryWorker();
        fetchFullRegistryWorker.start();
        this.fetchDeltaRegistryWorker.start();
    }

    /**
     * 销毁这个组件
     */
    public void destroy(){
        this.fetchDeltaRegistryWorker.interrupt();
    }

    /**
     * 全量拉取注册表的后台线程
     */
    private class FetchFullRegistryWorker extends Thread {
        @Override
        public void run() {
            // 拉取全量注册表
            // 这个操作要走网络，但是不知道为什么抽风了，此时就是一直卡住，数据没有返回回来
            // 卡在这儿了，卡了几分钟
            // 此时的这个数据是一个旧的版本，里面仅仅包含了30个服务实例
            // 全量拉注册表的线程突然苏醒过来了，此时将
            // 30个服务实例的旧版本的数据赋值给了本地缓存注册表

            // 一定要在发起网络请求之前，先拿到一个当时的版本号
            // 接着在这里发起网络请求，此时可能会有别的线程来修改这个注册表，更新版本，在这个期间
            // 必须是发起网络请求之后，这个注册表的版本没有被人修改过，此时他才能去修改
            // 如果在这个期间，有人修改过注册表，版本不一样了，此时就直接if不成立，不要把你拉取到的
            // 旧版本的注册表给设置进去
            fetchFullRegistry();
        }
    }

    /**
     * 拉取全量注册表到本地
     */
    private void fetchFullRegistry() {
        Long expectedVersion = applicationsVersion.get(); // version = 0
        Applications fetchedApplications = httpSender.fetchFullRegistry();

        if(applicationsVersion.compareAndSet(expectedVersion, expectedVersion + 1)) { // version = 1
            while(true) {
                Applications expectedApplications = applications.getReference();
                int expectedStamp = applications.getStamp();
                if(applications.compareAndSet(expectedApplications, fetchedApplications,
                        expectedStamp, expectedStamp + 1)) {
                    break;
                }
            }
        }
    }

    /**
     * 增量拉取注册表的后台线程
     */
    private class FetchDeltaRegistryWorker extends Thread {
        @Override
        public void run() {
            while (registerClient.isRunning()) {
                try {
                    Thread.sleep(SERVICE_REGISTRY_FETCH_INTERVAL);

                    // 拉取回来的是最近3分钟变化的服务实例
                    DeltaRegistry deltaRegistry = httpSender.fetchDeltaRegister();

                    // 一类是注册，一类是删除
                    // 如果是注册的话，就判断一下这个服务实例是否在这个本地缓存的注册表中
                    // 如果不在的话，就放到本地缓存注册表里去
                    // 如果是删除的话，就看一下，如果服务实例存在，就给删除了

                    // 我们这里其实是要大量的修改本地缓存的注册表，所以此处需要加锁
                    mergeDeltaRegistry(deltaRegistry.getRecentlyChangedQueue());

                    // 再检查一下，跟服务端的注册表的服务实例的数量相比，是否是一致的
                    // 封装一下增量注册表的对象，也就是拉取增量注册表的时候，一方面是返回那个数据
                    // 另外一方面，是要那个对应的register-server端的服务实例的数量
                    reconcileRegistry(deltaRegistry);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * 合并增量注册表到本地缓存注册表里去
         * @param deltaRegistry
         */
        private void mergeDeltaRegistry(LinkedList<RecentlyChangedServiceInstance> deltaRegistry) {
            Map<String, Map<String, ServiceInstance>> registry = applications.getReference().getRegistry();
            synchronized (applications) {
                for (RecentlyChangedServiceInstance recentlyChangedItem : deltaRegistry) {
                    String serviceName = recentlyChangedItem.serviceInstance.getServiceName();
                    String serviceInstanceId = recentlyChangedItem.serviceInstance.getServiceInstanceId();
                    Map<String, ServiceInstance> serviceInstanceMap = registry.get(serviceName);
                    // 如果是注册操作的话
                    if (ServiceInstanceOperation.REGISTER.equals(recentlyChangedItem.serviceInstanceOperation)) {

                        if (serviceInstanceMap == null) {
                            serviceInstanceMap = new HashMap<>();
                            registry.put(serviceName,serviceInstanceMap);
                        }

                        ServiceInstance serviceInstance = serviceInstanceMap.get(serviceInstanceId);
                        if(serviceInstance == null) {
                            serviceInstanceMap.put(
                                    serviceInstanceId,
                                    recentlyChangedItem.serviceInstance);
                        }
                    }
                    // 如果是删除操作的话
                    else if(ServiceInstanceOperation.REMOVE.equals(recentlyChangedItem.serviceInstanceOperation)) {
                        if(serviceInstanceMap != null) {
                            serviceInstanceMap.remove(serviceInstanceId);
                        }
                    }
                }
            }
        }
    }

    private void reconcileRegistry(DeltaRegistry deltaRegistry) {
        Map<String, Map<String, ServiceInstance>> registry = applications.getReference().getRegistry();
        Long serverSideTotalCount = deltaRegistry.getServiceInstanceTotalCount();

        Long clientSideTotalCount = 0L;
        for (Map<String, ServiceInstance> serviceInstanceMap : registry.values()) {
            clientSideTotalCount += serviceInstanceMap.size();
        }

        if (serverSideTotalCount != clientSideTotalCount) {
            // 重新拉取全量注册表进行纠正
            // 人家正常的进行了全量注册表最新数据的一个赋值，可能是包含了40个服务实例
            // 最新数据
            fetchFullRegistry();
        }
    }

    class ServiceInstanceOperation {
        /**
         * 注册
         */
        public static final String REGISTER = "register";
        /**
         * 删除
         */
        public static final String REMOVE = "REMOVE";

    }

    /**
     * 获取服务注册表
     * @return
     */
    public Map<String,Map<String,ServiceInstance>> getRegistry(){
        return applications.getReference().getRegistry();
    }

    /**
     * 最近变更的实例信息
     *
     */
    static class RecentlyChangedServiceInstance {

        /**
         * 服务实例
         */
        ServiceInstance serviceInstance;
        /**
         * 发生变更的时间戳
         */
        Long changedTimestamp;
        /**
         * 变更操作
         */
        String serviceInstanceOperation;

        public RecentlyChangedServiceInstance(
                ServiceInstance serviceInstance,
                Long changedTimestamp,
                String serviceInstanceOperation) {
            this.serviceInstance = serviceInstance;
            this.changedTimestamp = changedTimestamp;
            this.serviceInstanceOperation = serviceInstanceOperation;
        }

        @Override
        public String toString() {
            return "RecentlyChangedServiceInstance [serviceInstance=" + serviceInstance + ", changedTimestamp="
                    + changedTimestamp + ", serviceInstanceOperation=" + serviceInstanceOperation + "]";
        }

    }
}
