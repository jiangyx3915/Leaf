package com.sankuai.inf.leaf.segment.dao;

import com.sankuai.inf.leaf.segment.model.LeafWorkerIdAlloc;
import org.apache.ibatis.annotations.*;

/**
 * @author jiangyx3915
 */
public interface WorkerIdAllocMapper {

    /**
     * 新增记录
     * @param leafWorkerIdAlloc   LeafWorkerIdAlloc 对象
     * @return                    新增id
     */
    @Insert({
        "insert into leaf_workerid_alloc (ip_port," +
          "ip,port,max_timestamp,gmt_create,gmt_modified) ",
        "values (#{leafWorkId.ipPort} , #{leafWorkId.ip} , ",
        "#{leafWorkId.port} , #{leafWorkId.maxTimestamp} , ",
        "#{leafWorkId.gmtCreate} , #{leafWorkId.gmtModified} )"
    })
    @Options(useGeneratedKeys = true, keyProperty = "leafWorkId.id", keyColumn = "id")
    int insertIfNotExist(@Param("leafWorkId") LeafWorkerIdAlloc leafWorkerIdAlloc);

    /**
     * 根据ip port获取对象
     * @param ipPort      ip-port
     * @return            LeafWorkerIdAlloc 对象
     */
    @Select("select `id`,`port`,`ip`,`ip_port`,`max_timestamp` " +
      "from leaf_workerid_alloc where ip_port=#{ipPort};")
    @Results({
      @Result(column = "ip_port", property = "ipPort"),
      @Result(column = "max_timestamp", property = "maxTimestamp")
    })
    LeafWorkerIdAlloc getByIpPort(@Param("ipPort") String ipPort);

    /**
     * 更新最新时间戳
     * @param workerIdAlloc   LeafWorkerIdAlloc 对象
     */
    @Update("update leaf_workerid_alloc " +
      "set max_timestamp = #{workerIdAlloc.maxTimestamp} " +
      "where id = #{workerIdAlloc.id} ;")
    void updateMaxTimestamp(@Param("workerIdAlloc") LeafWorkerIdAlloc workerIdAlloc);
}
