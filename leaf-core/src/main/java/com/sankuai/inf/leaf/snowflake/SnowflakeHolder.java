package com.sankuai.inf.leaf.snowflake;

/**
 * 注册中心服务
 * @author mickle
 */
public interface SnowflakeHolder {

    /**
     * holder初始化方法
     * 执行init()成功，返回true之后才能调用getWorkerId获取workId
     * @return      是否初始化成功
     */
    boolean init();

    /**
     * 获取机器号
     * @return      机器号
     */
    long getWorkerId();

    /**
     * 获取机器ip
     * @return    ip地址
     */
    String getIp();

    /**
     * 获取机器端口
     * @return    端口号
     */
    String getPort();

    /**
     * 获取使用的模式
     * @return      模式
     */
    String getMode();
}
