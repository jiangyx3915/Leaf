package com.sankuai.inf.leaf.snowflake;

import com.sankuai.inf.leaf.IDGen;
import com.sankuai.inf.leaf.common.Result;
import com.sankuai.inf.leaf.common.Status;
import com.sankuai.inf.leaf.snowflake.exception.CheckHolderInitException;
import com.sankuai.inf.leaf.snowflake.exception.CheckLastTimeException;
import com.sankuai.inf.leaf.snowflake.exception.CheckOverloadWorkerIdException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 *
 * @author mickle
 */
public class SnowflakeIDGenImpl implements IDGen {

    @Override
    public boolean init() {
        return true;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeIDGenImpl.class);

    private final long twepoch = 1288834974657L;
    private final long workerIdBits = 10L;
    /**
     * 最大能够分配的workerid =1023
     */
    private final long maxWorkerId = ~(-1L << workerIdBits);
    private final long sequenceBits = 12L;
    private final long workerIdShift = sequenceBits;
    private final long timestampLeftShift = sequenceBits + workerIdBits;
    private final long sequenceMask = ~(-1L << sequenceBits);
    private long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    private static final Random RANDOM = new Random();

    public SnowflakeIDGenImpl(SnowflakeHolder snowflakeHolder) {
        if (timeGen() <= twepoch) {
            throw new CheckLastTimeException("Snowflake not support twepoch gt currentTime");
        }
        LOGGER.info("register twepoch:{} ,ip:{}, port:{}", twepoch,
          snowflakeHolder.getIp(), snowflakeHolder.getPort());
        boolean initFlag = snowflakeHolder.init();
        if (initFlag) {
            workerId = snowflakeHolder.getWorkerId();
            LOGGER.info("START SUCCESS USE ZK WORKERID-{}", workerId);
        } else {
            throw new CheckHolderInitException("Snowflake Mode " + snowflakeHolder.getMode() + " Id Gen is not init ok");
        }
        if (workerId < 0 || workerId > maxWorkerId) {
            throw new CheckOverloadWorkerIdException("workerID must gte 0 and lte 1023");
        }
    }

    @Override
    public synchronized Result get(String key) {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                try {
                    wait(offset << 1);
                    timestamp = timeGen();
                    if (timestamp < lastTimestamp) {
                        return new Result(-1, Status.EXCEPTION);
                    }
                } catch (InterruptedException e) {
                    LOGGER.error("wait interrupted");
                    return new Result(-2, Status.EXCEPTION);
                }
            } else {
                return new Result(-3, Status.EXCEPTION);
            }
        }
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                //seq 为0的时候表示是下一毫秒时间开始对seq做随机
                sequence = RANDOM.nextInt(100);
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            //如果是新的ms开始
            sequence = RANDOM.nextInt(100);
        }
        lastTimestamp = timestamp;
        long id = ((timestamp - twepoch) << timestampLeftShift) | (workerId << workerIdShift) | sequence;
        return new Result(id, Status.SUCCESS);

    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    protected long timeGen() {
        return System.currentTimeMillis();
    }

    public long getWorkerId() {
        return workerId;
    }

}
