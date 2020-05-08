package com.mine.server.namenode;

import java.util.LinkedList;

/**
 * 负责管理edits log日志的核心组件
 */
public class FSEditlog {
    /**
     * 当前递增到的txid的序号
     */
    private long txidSeq = 0L;
    /**
     * 内存双缓冲区
     */
    private DoubleBuffer editBuffer = new DoubleBuffer();
    /**
     * 当前是否在将内存缓冲刷入磁盘中
     */
    private volatile boolean isSyncRunning = false;
    /**
     * 当前是否有线程在等待刷新下一批edits log到磁盘里去
     */
    private volatile boolean isWaitSync = false;
    /**
     * 在同步到磁盘中的最大的一个txid
     */
    private volatile long syncMaxTxid = 0L;
    /**
     * 每个线程自己本地的txid副本
     */
    private ThreadLocal<Long> localTxid = new ThreadLocal<Long>();

    /**
     * 记录edits log日志
     */
    public void logEdit(String content) {
        // 这里必须得直接加锁
        synchronized (this){
            // 获取全局唯一递增的txid，代表了edits log的序号
            txidSeq ++;
            long txid = txidSeq;
            localTxid.set(txid);

            // 构造一条edits log对象
            Editlog editlog = new Editlog(txid, content);

            // 将edits log写入内存缓冲中，不是直接刷入磁盘文件
            editBuffer.write(editlog);
        }
        logSync();
    }

    /**
     * 将内存缓冲中的数据刷入磁盘文件中
     * 在这里尝试允许某一个线程一次性将内存缓冲中的数据刷入磁盘文件中
     * 相当于实现一个批量将内存缓冲数据刷磁盘的过程
     */
    private void logSync(){
        // 再次尝试加锁
        synchronized (this) {
            // 如果说当前正好有人在刷内存缓冲到磁盘中去
            if (isSyncRunning) {
                // 那么此时这里应该有一些逻辑判断

                // 假如说某个线程已经把txid = 1,2,3,4,5的edits log都从syncBuffer刷入磁盘了
                // 或者说此时正在刷入磁盘中
                // 此时syncMaxTxid = 5，代表的是正在输入磁盘的最大txid
                // 那么这个时候来一个线程，他对应的txid = 3，此时他是可以直接返回了
                // 就代表说肯定是他对应的edits log已经被别的线程在刷入磁盘了
                // 这个时候txid = 3的线程就不需要等待了
                if (syncMaxTxid >= localTxid.get()) {
                    return;
                }

                //如果已经有线程在等待了，就直接返回
                if (isWaitSync){
                    return;
                }

                isWaitSync = true;
                while (isSyncRunning){
                    try {
                        wait(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                isWaitSync = false;
            }
            // 交换两块缓冲区
            editBuffer.setReadyToSync();
            // 然后可以保存一下当前要同步到磁盘中去的最大的txid
            // 此时editLogBuffer中的syncBuffer这块区域，交换完以后这里可能有多条数据
            // 而且他里面的edits log的txid一定是从小到大的
            // 此时要同步的txid = 6,7,8,9,10,11,12
            // syncMaxTxid = 12
            syncMaxTxid = editBuffer.getSyncMaxTxid();

            // 设置当前正在同步到磁盘的标志位
            isSyncRunning = true;
        }

        // 开始同步内存缓冲的数据到磁盘文件里去
        // 这个过程其实是比较慢，基本上肯定是毫秒级了，弄不好就要几十毫秒
        editBuffer.flush();

        synchronized (this) {
            // 同步完了磁盘之后，就会将标志位复位，再释放锁
            isSyncRunning = false;
            // 唤醒可能正在等待他同步完磁盘的线程
            notifyAll();
        }
    }

    /**
     * 代表了一条edits log
     */
    class Editlog{
        private long txid;
        private String content;

        public Editlog(long txid, String content) {
            this.txid = txid;
            this.content = content;
        }
    }

    /**
     * 内存双缓冲
     */
    class DoubleBuffer{
        /**
         * 是专门用来承载线程写入edits log
         */
        LinkedList<Editlog> currentBuffer = new LinkedList<Editlog>();
        /**
         * 专门用来将数据同步到磁盘中去的一块缓冲
         */
        LinkedList<Editlog> syncBuffer = new LinkedList<Editlog>();

        /**
         * 将edits log写到内存缓冲里去
         * @param log
         */
        public void write(Editlog log){
            currentBuffer.add(log);
        }

        /**
         * 交换两块缓冲区，为了同步内存数据到磁盘做准备
         */
        public void setReadyToSync(){
            LinkedList<Editlog> tmp = currentBuffer;
            currentBuffer = syncBuffer;
            syncBuffer = tmp;
        }

        /**
         * 获取sync buffer缓冲区里的最大的一个txid
         * @return
         */
        public long getSyncMaxTxid(){
            return syncBuffer.getLast().txid;
        }

        /**
         * 将syncBuffer缓冲区中的数据刷入磁盘中
         */
        public void flush(){
            for (Editlog log : syncBuffer) {
                System.out.println("将edit log写入磁盘文件中：" + log);
                // 正常来说，就是用文件输出流将数据写入磁盘文件中
            }
            syncBuffer.clear();
        }

    }
}
