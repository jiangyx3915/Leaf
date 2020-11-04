package com.sankuai.inf.leaf.snowflake;

import com.sankuai.inf.leaf.common.PropertyFactory;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * SnowflakeHolder抽象类
 * @author mickle
 */
public abstract class AbstractSnowflakeHolder implements SnowflakeHolder {
    protected String ip;
    protected String port;
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractSnowflakeHolder.class);
    protected static final String PROP_PATH = System.getProperty("java.io.tmpdir") +
      PropertyFactory.getProperties().getProperty("leaf.name") + "/leaf_conf/{port}/workerId.properties";

    /**
     * 在节点文件系统上缓存一个workerId值,zk失效,机器重启时保证能够正常启动
     *
     * @param workerId    机器号
     */
    protected void updateLocalWorkerId(long workerId) {
        File leafConfFile = new File(PROP_PATH.replace("{port}", port.toString()));
        boolean exists = leafConfFile.exists();
        LOGGER.info("file exists status is {}", exists);
        if (exists) {
            try {
                FileUtils.writeStringToFile(leafConfFile, "workerId=" + workerId, false);
                LOGGER.info("update file cache workerId is {}", workerId);
            } catch (IOException e) {
                LOGGER.error("update file cache error ", e);
            }
        } else {
            //不存在文件,父目录页肯定不存在
            try {
                boolean mkdirs = leafConfFile.getParentFile().mkdirs();
                LOGGER.info("init local file cache create parent dis status is {}, worker id is {}", mkdirs, workerId);
                if (mkdirs) {
                    if (leafConfFile.createNewFile()) {
                    FileUtils.writeStringToFile(leafConfFile, "workerId=" + workerId, false);
                    LOGGER.info("local file cache workerId is {}", workerId);
                }
              } else {
                  LOGGER.warn("create parent dir error===");
              }
            } catch (IOException e) {
                LOGGER.warn("create workerId conf file error", e);
            }
        }
    }

    /**
     * 定时上传数据到注册中心
     */
    protected abstract void scheduledUploadData();

    /**
     * 读取本地文件中缓存的机器号
     * @return      机器号
     */
    protected Integer readLocalFile() {
          try {
              Properties properties = new Properties();
              properties.load(new FileInputStream(new File(PROP_PATH.replace("{port}", port + ""))));
              Integer workerId = Integer.parseInt(properties.getProperty("workerId"));
              LOGGER.warn("START FAILED ,use local node file properties workerId-{}", workerId);
              return workerId;
          } catch (Exception e) {
              LOGGER.error("Read file error ", e);
              return null;
          }
      }
}
