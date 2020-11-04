package com.sankuai.inf.leaf.server.service;

import com.alibaba.druid.pool.DruidDataSource;
import com.sankuai.inf.leaf.IDGen;
import com.sankuai.inf.leaf.common.PropertyFactory;
import com.sankuai.inf.leaf.common.Result;
import com.sankuai.inf.leaf.common.Utils;
import com.sankuai.inf.leaf.common.ZeroIDGen;
import com.sankuai.inf.leaf.segment.dao.WorkerIdAllocDao;
import com.sankuai.inf.leaf.segment.dao.impl.WorkerIdAllocDaoImpl;
import com.sankuai.inf.leaf.server.Constants;
import com.sankuai.inf.leaf.server.SnowflakeMode;
import com.sankuai.inf.leaf.server.exception.InitException;
import com.sankuai.inf.leaf.server.exception.NoModelException;
import com.sankuai.inf.leaf.snowflake.SnowflakeIDGenImpl;
import com.sankuai.inf.leaf.snowflake.SnowflakeLocalHolder;
import com.sankuai.inf.leaf.snowflake.SnowflakeMySqlHolder;
import com.sankuai.inf.leaf.snowflake.SnowflakeZookeeperHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Snowflake Service类
 * @author jiangyx3915
 */
@Service("SnowflakeService")
public class SnowflakeService {
    private final Logger logger = LoggerFactory.getLogger(SnowflakeService.class);

    private final IDGen idGen;

    public SnowflakeService() throws InitException, SQLException, IOException {
        Properties properties = PropertyFactory.getProperties();
        boolean flag = Boolean.parseBoolean(properties.getProperty(Constants.LEAF_SNOWFLAKE_ENABLE, "true"));
        String mode = properties.getProperty(Constants.LEAF_SNOWFLAKE_MODE, SnowflakeMode.ZK);
        if (flag) {
            final String ip = Utils.getIp();
            final String port = properties.getProperty(Constants.LEAF_SNOWFLAKE_PORT);
            switch (mode) {
                case SnowflakeMode.MYSQL:
                    DruidDataSource dataSource = new DruidDataSource();
                    dataSource.setUrl(properties.getProperty(Constants.LEAF_SNOWFLAKE_MYSQL_JDBC_URL));
                    dataSource.setUsername(properties.getProperty(Constants.LEAF_SNOWFLAKE_MYSQL_USERNAME));
                    dataSource.setPassword(properties.getProperty(Constants.LEAF_SNOWFLAKE_MYSQL_PASSWORD));
                    dataSource.init();
                    WorkerIdAllocDao workerIdAllocDao = new WorkerIdAllocDaoImpl(dataSource);
                    SnowflakeMySqlHolder mySqlHolder = new SnowflakeMySqlHolder(ip, port, workerIdAllocDao);
                    idGen = new SnowflakeIDGenImpl(mySqlHolder);
                    break;
                case SnowflakeMode.ZK:
                    String zkAddress = properties.getProperty(Constants.LEAF_SNOWFLAKE_ZK_ADDRESS);
                    SnowflakeZookeeperHolder zookeeperHolder = new SnowflakeZookeeperHolder(ip, port, zkAddress);
                    idGen = new SnowflakeIDGenImpl(zookeeperHolder);
                    break;
                case SnowflakeMode.LOCAL:
                    String workerIdMap = properties.getProperty(Constants.LEAF_SNOWFLAKE_LOCAL_WORKERID_MAP);
                    SnowflakeLocalHolder localHolder = new SnowflakeLocalHolder(ip, port, workerIdMap);
                    idGen = new SnowflakeIDGenImpl(localHolder);
                    break;
                default:
                    throw new NoModelException();
            }
            if(idGen.init()) {
                logger.info("Snowflake Service Mode {} Init Successfully", mode);
            } else {
                throw new InitException("Snowflake Service " + mode + " Init Fail");
            }
        } else {
            idGen = new ZeroIDGen();
            logger.info("Zero ID Gen Service Init Successfully");
        }
    }

    public Result getId(String key) {
        return idGen.get(key);
    }
}
