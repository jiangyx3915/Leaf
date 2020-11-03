package com.sankuai.inf.leaf.segment.model;

import java.util.Date;

/**
 * @author jiangyx3915
 */
public class LeafWorkerIdAlloc {
    private Integer id;
    private String ip;
    private String port;
    private String ipPort;
    private Long maxTimestamp;
    private Date gmtCreate;
    private Date gmtModified;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
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

  public String getIpPort() {
    return ipPort;
  }

  public void setIpPort(String ipPort) {
    this.ipPort = ipPort;
  }

  public Long getMaxTimestamp() {
    return maxTimestamp;
  }

  public void setMaxTimestamp(Long maxTimestamp) {
    this.maxTimestamp = maxTimestamp;
  }

  public Date getGmtCreate() {
    return gmtCreate;
  }

  public void setGmtCreate(Date gmtCreate) {
    this.gmtCreate = gmtCreate;
  }

  public Date getGmtModified() {
    return gmtModified;
  }

  public void setGmtModified(Date gmtModified) {
    this.gmtModified = gmtModified;
  }
}
