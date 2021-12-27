package com.github.yiYangGit.tcpproxy;

import io.netty.channel.ChannelOption;

import javax.net.ssl.SSLContext;
import java.util.HashMap;
import java.util.Map;

/**
 * 代理相关配置类
 * <p>
 * Created by yangyi on 2021/12/22.
 */
public class ProxyOptionConfig {

    private Map<ChannelOption<Object>, Object> options = new HashMap<>();
    public <T> void nettyOption(ChannelOption<Object> option, Object value) {
        this.options.put(option,value);
    }

    public Map<ChannelOption<Object>, Object> getOptions() {
        return options;
    }

    /**
     * 如果需要ssl通信,则需要提供 SSLContext
     */
    private SSLContext sslContext;

    /**
     * 是否需要验证客户端证书
     */
    private Boolean needClientAuth;

    public SSLContext getSslContext() {
        return sslContext;
    }

    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    public Boolean getNeedClientAuth() {
        return needClientAuth;
    }

    public void setNeedClientAuth(Boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
    }
}
