package com.github.yiYangGit.tcpproxy;

import java.util.concurrent.TimeUnit;

/**
 * Created by yangyi on 2021/12/22.
 */
public class ProxyConfig {

    /**
     * 代理默认的最大线程数
     */
    public static final int PROXY_DEFAULT_MAX_IO_THREADS = 8;


    /**
     * 服务端作为代理层,如果长时间未接收到客户端或者代理后端的数据包,
     * 那么服务端将会杀掉当前的 客户端和代理后端的连接
     * 此单位为毫秒 <=0 则服务端不会检测该连接,默认为 0L
     */
    private long keepAliveMilliSecond = 0L;


    /**
     * 客户端断开连接后,代理后端没有断开连接,那么服务端将尝试将剩余的缓存的数据包发送到代理后端
     * 但是在超过这个时间后将会直接会断开和代理后端的连接
     * <=0 立刻断开连接
     */
    private long keepAliveBackendMilliSecond = TimeUnit.SECONDS.toMillis(10);


    /**
     * 代理后端断开连接后,前端没有断开连接,那么服务端将尝试将剩余的缓存的数据包发送到前端
     * 但是在超过这个时间后将会直接会断开和前端的连接
     * <=0 立刻断开连接
     */
    private long keepAliveFrontendMilliSecond = TimeUnit.SECONDS.toMillis(10);


    /**
     * 服务器支持的最大同时的连接数
     * <=0 则不做最大连接的限制
     * 此处的连接指的是代理和客户端可以建立的最大连接数
     */
    private int maxConnects = ConnectEndpoint.NOT_LIMIT_NUMBER;

    /**
     * io线程数,本代理服务最终会启动一个线程用于接收socket连接
     * 然后在启动 ioThreads 数量线程用于网络IO,
     */
    private int ioThreads = Math.max(Runtime.getRuntime().availableProcessors(), PROXY_DEFAULT_MAX_IO_THREADS);

    private ServerSocketConfig serverSocketConfig = new ServerSocketConfig();


    private ExceptionFilter exceptionFilter;
    /**
     * 前端的Tcp连接配置
     */
    private ProxyOptionConfig proxyFrontendOptionConfig = new ProxyOptionConfig();

    /**
     * 代理后端的Tcp连接配置
     */
    private ProxyOptionConfig proxyBackendOptionConfig = new ProxyOptionConfig();

    public ExceptionFilter getExceptionFilter() {
        return exceptionFilter;
    }

    public ProxyConfig setExceptionFilter(ExceptionFilter exceptionFilter) {
        this.exceptionFilter = exceptionFilter;
        return this;
    }

    public int getIoThreads() {
        return ioThreads;
    }

    public void setIoThreads(int ioThreads) {
        this.ioThreads = ioThreads;
    }

    public long getKeepAliveMilliSecond() {
        return keepAliveMilliSecond;
    }

    public void setKeepAliveMilliSecond(long keepAliveMilliSecond) {
        this.keepAliveMilliSecond = keepAliveMilliSecond;
    }

    public long getKeepAliveBackendMilliSecond() {
        return keepAliveBackendMilliSecond;
    }

    public void setKeepAliveBackendMilliSecond(long keepAliveBackendMilliSecond) {
        this.keepAliveBackendMilliSecond = keepAliveBackendMilliSecond;
    }

    public long getKeepAliveFrontendMilliSecond() {
        return keepAliveFrontendMilliSecond;
    }

    public void setKeepAliveFrontendMilliSecond(long keepAliveFrontendMilliSecond) {
        this.keepAliveFrontendMilliSecond = keepAliveFrontendMilliSecond;
    }


    public int getMaxConnects() {
        return maxConnects;
    }

    public void setMaxConnects(int maxConnects) {
        this.maxConnects = maxConnects;
    }

    public ProxyOptionConfig getProxyBackendOptionConfig() {
        return proxyBackendOptionConfig;
    }

    public void setProxyBackendOptionConfig(ProxyOptionConfig proxyBackendOptionConfig) {
        this.proxyBackendOptionConfig = proxyBackendOptionConfig;
    }

    public ServerSocketConfig getServerSocketConfig() {
        return serverSocketConfig;
    }

    public void setServerSocketConfig(ServerSocketConfig serverSocketConfig) {
        this.serverSocketConfig = serverSocketConfig;
    }

    public ProxyOptionConfig getProxyFrontendOptionConfig() {
        return proxyFrontendOptionConfig;
    }

    public void setProxyFrontendOptionConfig(ProxyOptionConfig proxyFrontendOptionConfig) {
        this.proxyFrontendOptionConfig = proxyFrontendOptionConfig;
    }
}
