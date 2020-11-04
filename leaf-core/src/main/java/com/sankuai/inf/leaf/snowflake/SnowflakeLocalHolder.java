package com.sankuai.inf.leaf.snowflake;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sankuai.inf.leaf.snowflake.exception.ParseMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * 本地模式
 *
 * 在配置文件中配置 ip-port:机器号 列表，适用于机器固定的情况
 * @author mickle
 */
public class SnowflakeLocalHolder extends AbstractSnowflakeHolder {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeLocalHolder.class);
    private int workerId;
    private Map<String, Integer> workerIdMap;

    public SnowflakeLocalHolder(String ip, String port, String mapping) throws IOException {
        this.ip = ip;
        this.port = port;
        this.parseWorkerIdMap(mapping);
    }

    /**
     * 解析ip端口号和机器号的映射
     * @param workerIdMapping   映射关系
     */
    private void parseWorkerIdMap(String workerIdMapping) {
        if (workerIdMapping == null) {
            LOGGER.error("Not set workerIdMap");
            throw new RuntimeException("Not set workerIdMap");
        }
        try{
            ObjectMapper mapper = new ObjectMapper();
            this.workerIdMap = mapper.readValue(workerIdMapping, Map.class);
        } catch (Exception e) {
            throw new ParseMappingException("Parse workerIdMapping error, " + e);
        }

    }

    @Override
    protected void scheduledUploadData() {
    }

    @Override
    public boolean init() {
        String ipPort = ip + ":" + port;
        Integer mapperWorkerId = workerIdMap.get(ipPort);
        if (mapperWorkerId != null) {
          workerId = mapperWorkerId;
          updateLocalWorkerId(workerId);
          return true;
        }
        LOGGER.error("leaf.properties not set {} workerId", ipPort);
        Integer localWorkerId = readLocalFile();
        if (localWorkerId == null) {
          return false;
        }
        this.workerId = localWorkerId;
        return true;
    }

    @Override
    public long getWorkerId() {
      return this.workerId;
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
      return "local";
    }
}
