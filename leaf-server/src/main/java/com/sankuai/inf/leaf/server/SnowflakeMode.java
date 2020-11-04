package com.sankuai.inf.leaf.server;

/**
 *  ZK 注册中心为Zookeeper，,针对每个ip:port的workerId是固定的
 *  就是根据ip+port在zookeeper创建一个永久的workId
 *
 *  MYSQL 注册中心为MySQL,针对每个ip:port的workerId是固定的
 *
 * @author jiangyx3915
 *
 */
public class SnowflakeMode {
  public static final String ZK = "zk";
  public static final String MYSQL = "mysql";
  public static final String LOCAL = "local";
}
