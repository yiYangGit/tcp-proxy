package com.github.yiYangGit.tcpproxy;


/**
 * 为代理服务定义的接口
 * Created by yangyi on 2021/12/24.
 */
public interface ProxyEventHandler {


    /**
     * 代理的另一端读取到数据流
     * @param byteBuf
     */
    void onRemoteRead(Object byteBuf);

    /**
     * netty中一批次数据读取完成后 触发的 ReadComplete时间
     */
    void onRemoteReadComplete();

    /**
     * 处理代理的另一端连接关闭
     */
    void onRemoteConnectClose();

    /**
     * 对端可写状态关闭,netty中缓存的数据变为高水位
     */
    void onRemoteWriteStatusDisable();

    /**
     * 对端可写状态变为开启状态
     */
    void onRemoteWriteStatusEnable();

}
