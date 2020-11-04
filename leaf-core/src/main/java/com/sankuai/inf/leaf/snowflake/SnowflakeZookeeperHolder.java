package com.sankuai.inf.leaf.snowflake;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sankuai.inf.leaf.snowflake.exception.CheckLastTimeException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.retry.RetryUntilElapsed;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.sankuai.inf.leaf.common.*;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.zookeeper.CreateMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 以Zookeeper为注册中心
 * @author jiangyx3915
 */
public class SnowflakeZookeeperHolder extends AbstractSnowflakeHolder {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeZookeeperHolder.class);
    /**
     * 保存自身的key  ip:port-000000001
     */
    private String zkAddressNode = null;
    /**
     * 保存自身的key ip:port
     */
    private String listenAddress = null;
    private static final String PREFIX_ZK_PATH = "/snowflake/" +
      PropertyFactory.getProperties().getProperty("leaf.name");
    /**
     * 保存所有数据持久的节点
     */
    private static final String PATH_FOREVER = PREFIX_ZK_PATH + "/forever";
    private final String connectionString;
    private long lastUpdateTime;
    private long workerId;
    private CuratorFramework curator;

    public SnowflakeZookeeperHolder(String ip, String port, String connectionString) {
        this.ip = ip;
        this.port = port;
        this.listenAddress = ip + ":" + port;
        this.connectionString = connectionString;
    }

    @Override
    public boolean init() {
        try {
            curator = createWithOptions(connectionString,
              new RetryUntilElapsed(1000, 4),
              10000, 6000);
            curator.start();
            Stat stat = curator.checkExists().forPath(PATH_FOREVER);
            if (stat == null) {
                //不存在根节点,机器第一次启动,创建/snowflake/ip:port-000000000,并上传数据
                zkAddressNode = createNode(curator);
                //worker id 默认是0
                updateLocalWorkerId(workerId);
                //定时上报本机时间给forever节点
                scheduledUploadData();
                return true;
            } else {
                // ip:port->00001
                Map<String, Integer> nodeMap = Maps.newHashMap();
                // ip:port->(ip:port-000001)
                Map<String, String> realNode = Maps.newHashMap();
                //存在根节点,先检查是否有属于自己的根节点
                List<String> keys = curator.getChildren().forPath(PATH_FOREVER);
                for (String key : keys) {
                    String[] nodeKey = key.split("-");
                    realNode.put(nodeKey[0], key);
                    nodeMap.put(nodeKey[0], Integer.parseInt(nodeKey[1]));
                }
                Integer workerid = nodeMap.get(listenAddress);
                if (workerid != null) {
                    //有自己的节点,zk_AddressNode=ip:port
                    zkAddressNode = PATH_FOREVER + "/" + realNode.get(listenAddress);
                    //启动机器时使用会使用
                    workerId = workerid;
                    if (!checkInitTimeStamp(curator, zkAddressNode)) {
                        throw new CheckLastTimeException("init timestamp check error,forever node timestamp gt this node time");
                    }
                    //准备创建临时节点
                    scheduledUploadData();
                    updateLocalWorkerId(workerId);
                    LOGGER.info("[Old NODE]find forever node have this endpoint ip-{} port-{} workid-{} childnode and start SUCCESS", ip, port, workerid);
                } else {
                    //表示新启动的节点,创建持久节点 ,不用check时间
                    String newNode = createNode(curator);
                    zkAddressNode = newNode;
                    String[] nodeKey = newNode.split("-");
                    workerid = Integer.parseInt(nodeKey[1]);
                    scheduledUploadData();
                    updateLocalWorkerId(workerId);
                    LOGGER.info("[New NODE]can not find node on forever node that endpoint ip-{} port-{} workid-{},create own node on forever node and start SUCCESS ", ip, port, workerid);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Start node ERROR {0}", e);
            Integer localWorkerId = readLocalFile();
            if (localWorkerId == null) {
                return false;
            }
            this.workerId = localWorkerId;
        }
        return true;
    }

    private boolean checkInitTimeStamp(CuratorFramework curator, String zkAddressNode) throws Exception {
        byte[] bytes = curator.getData().forPath(zkAddressNode);
        Endpoint endPoint = deBuildData(new String(bytes));
        //该节点的时间不能小于最后一次上报的时间
        return endPoint.getTimestamp() <= System.currentTimeMillis();
    }

    /**
     * 创建持久顺序节点 ,并把节点数据放入 value
     *
     * @param curator
     * @return
     * @throws Exception
     */
    private String createNode(CuratorFramework curator) throws Exception {
        try {
            return curator.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT_SEQUENTIAL)
              .forPath(PATH_FOREVER + "/" + listenAddress + "-", buildData().getBytes());
        } catch (Exception e) {
            LOGGER.error("create node error msg {} ", e.getMessage());
            throw e;
        }
    }

    private void updateNewData(CuratorFramework curator, String path) {
        try {
            if (System.currentTimeMillis() < lastUpdateTime) {
                return;
            }
            curator.setData().forPath(path, buildData().getBytes());
            lastUpdateTime = System.currentTimeMillis();
        } catch (Exception e) {
            LOGGER.info("update init data error path is {} error is {}", path, e);
        }
    }

    /**
     * 构建需要上传的数据
     *
     * @return
     */
    private String buildData() throws JsonProcessingException {
        Endpoint endpoint = new Endpoint(ip, port, System.currentTimeMillis());
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(endpoint);
    }

    private Endpoint deBuildData(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Endpoint.class);
    }

    private CuratorFramework createWithOptions(String connectionString, RetryPolicy retryPolicy, int connectionTimeoutMs, int sessionTimeoutMs) {
        return CuratorFrameworkFactory.builder().connectString(connectionString)
                .retryPolicy(retryPolicy)
                .connectionTimeoutMs(connectionTimeoutMs)
                .sessionTimeoutMs(sessionTimeoutMs)
                .build();
    }

    @Override
    protected void scheduledUploadData() {
      //每3s上报数据
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
          updateNewData(curator, zkAddressNode);
        }
      },
        1L, 3L, TimeUnit.SECONDS);
    }

  /**
     * 上报数据结构
     */
    static class Endpoint {
        private String ip;
        private String port;
        private long timestamp;

        public Endpoint() {
        }

        public Endpoint(String ip, String port, long timestamp) {
            this.ip = ip;
            this.port = port;
            this.timestamp = timestamp;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    public String getZkAddressNode() {
        return zkAddressNode;
    }

    public void setZkAddressNode(String zkAddressNode) {
        this.zkAddressNode = zkAddressNode;
    }

    public String getListenAddress() {
        return listenAddress;
    }

    public void setListenAddress(String listenAddress) {
        this.listenAddress = listenAddress;
    }

    @Override
    public long getWorkerId() {
      return workerId;
    }

    @Override
    public String getIp() {
      return this.ip;
    }

    @Override
    public String getPort() {
      return this.port;
    }

    @Override
    public String getMode() {
      return "zk";
    }

    public void setWorkerId(long workerId) {
      this.workerId = workerId;
    }
}
