package com.sankuai.inf.leaf.segment.dao;

import com.sankuai.inf.leaf.segment.model.LeafWorkerIdAlloc;

/**
 *
 * @author jiangyx3915
 */
public interface WorkerIdAllocDao {

    /**
     * 获取id
     * @param ip      本机ip
     * @param port    本机port
     * @return        LeafWorkerIdAlloc对象
     */
    LeafWorkerIdAlloc getOrCreateWorkId(String ip, String port);

    /**
     * 更新最近同步时间
     * @param workerId      机器号
     * @param timestamp     当前时间戳
     */
    void updateMaxTimestamp(Integer workerId, Long timestamp);
}
