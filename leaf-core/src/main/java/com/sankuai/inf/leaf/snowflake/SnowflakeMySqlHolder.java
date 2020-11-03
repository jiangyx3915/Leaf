package com.sankuai.inf.leaf.snowflake;

import com.sankuai.inf.leaf.segment.dao.WorkerIdAllocDao;
import com.sankuai.inf.leaf.segment.model.LeafWorkerIdAlloc;
import com.sankuai.inf.leaf.snowflake.exception.CheckLastTimeException;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 以MySQL为注册中心
 * @author jiangyx3915
 */
public class SnowflakeMySqlHolder extends AbstractSnowflakeHolder {
    private final WorkerIdAllocDao allocDao;
    private int workerId;
    private long lastUpdateTime;

    public SnowflakeMySqlHolder(String ip, String port, WorkerIdAllocDao allocDao) {
        this.ip = ip;
        this.port = port;
        this.allocDao = allocDao;
    }

    @Override
    public boolean init() {
        // 获取workerId并缓存到本地文件
        try{
            LeafWorkerIdAlloc alloc = allocDao.getOrCreateWorkId(this.ip, this.port);
            if (alloc.getMaxTimestamp() > System.currentTimeMillis()) {
              throw new CheckLastTimeException("init timestamp check error,db node timestamp gt this node time");
            }
            this.workerId = alloc.getId();
            updateLocalWorkerId(workerId);
            scheduledUploadData();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public long getWorkerId() {
      return this.workerId;
    }

    @Override
    protected void scheduledUploadData() {
        Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
          @Override
          public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "schedule-upload-time");
            thread.setDaemon(true);
            return thread;
          }
        }).scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
              updateNewData();
            }
          },
          1L, 3L, TimeUnit.SECONDS);
    }

    private void updateNewData() {
        try {
            if (System.currentTimeMillis() < lastUpdateTime) {
              return;
            }
            allocDao.updateMaxTimestamp(workerId, System.currentTimeMillis());
            this.lastUpdateTime = System.currentTimeMillis();
        } catch (Exception e) {
          LOGGER.info("update data error, is error is {0}", e);
        }
    }
}
