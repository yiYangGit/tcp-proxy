package com.github.yiYangGit.tcpproxy;

/**
 * Created by yangyi on 2021/12/22.
 */
public class ServerSocketConfig {

    /**
     * 服务端ServerSocket的配置
     * 实际调用了
     * java.net.ServerSocket#setReceiveBufferSize(int)
     * 为null 则不配置 ,使用系统默认值
     */
    private Integer rxBufSize = null;

    /**
     * 服务端是否重用地址
     * java.net.ServerSocket#setReuseAddress(boolean)
     * 为null 则不配置
     */
    private Boolean soReuseAddress = null;


    /**
     * 服务端backlog属性
     * 默认值为100
     */
    private int serverBacklog = 1000;

    public Integer getRxBufSize() {
        return rxBufSize;
    }

    public ServerSocketConfig setRxBufSize(Integer rxBufSize) {
        this.rxBufSize = rxBufSize;
        return this;
    }

    public Boolean getSoReuseAddress() {
        return soReuseAddress;
    }

    public ServerSocketConfig setSoReuseAddress(Boolean soReuseAddress) {
        this.soReuseAddress = soReuseAddress;
        return this;
    }

    public int getServerBacklog() {
        return serverBacklog;
    }

    public ServerSocketConfig setServerBacklog(int serverBacklog) {
        this.serverBacklog = serverBacklog;
        return this;
    }
}
