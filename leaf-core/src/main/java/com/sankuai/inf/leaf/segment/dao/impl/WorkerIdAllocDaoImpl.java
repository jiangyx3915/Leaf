package com.sankuai.inf.leaf.segment.dao.impl;

import com.sankuai.inf.leaf.common.Utils;
import com.sankuai.inf.leaf.segment.dao.WorkerIdAllocDao;
import com.sankuai.inf.leaf.segment.dao.WorkerIdAllocMapper;
import com.sankuai.inf.leaf.segment.model.LeafWorkerIdAlloc;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.util.Date;

/**
 * @author jiangyx3915
 */
public class WorkerIdAllocDaoImpl implements WorkerIdAllocDao {

    private final SqlSessionFactory sqlSessionFactory;

    public WorkerIdAllocDaoImpl(DataSource dataSource) {
        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(WorkerIdAllocMapper.class);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }


    @Override
    public LeafWorkerIdAlloc getOrCreateWorkId(String ip, String port) {
        String ipPort = ip + ":" + port;
        try (SqlSession sqlSession = sqlSessionFactory.openSession()){
            WorkerIdAllocMapper mapper = sqlSession.getMapper(WorkerIdAllocMapper.class);
            LeafWorkerIdAlloc alloc = mapper.getByIpPort(ipPort);
            if (alloc != null) {
                return alloc;
            }
            Date now = new Date();
            alloc = new LeafWorkerIdAlloc();
            alloc.setIp(ip);
            alloc.setPort(port);
            alloc.setIpPort(ipPort);
            alloc.setMaxTimestamp(System.currentTimeMillis());
            alloc.setGmtCreate(now);
            alloc.setGmtModified(now);
            mapper.insertIfNotExist(alloc);
            sqlSession.commit();
            return alloc;
        }
    }

    @Override
    public void updateMaxTimestamp(Integer workerId, Long timestamp) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()){
            WorkerIdAllocMapper mapper = sqlSession.getMapper(WorkerIdAllocMapper.class);
            LeafWorkerIdAlloc alloc = new LeafWorkerIdAlloc();
            alloc.setId(workerId);
            alloc.setMaxTimestamp(timestamp);
            mapper.updateMaxTimestamp(alloc);
            sqlSession.commit();
        }
    }
}
