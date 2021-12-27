package com.github.yiYangGit.tcpproxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;

public class FrontendConnectInitHandler extends ConnectActiveInitHandler {

    private static final Logger logger = LoggerFactory.getLogger(FrontendConnectInitHandler.class);

    private final ProxyConfig proxyConfig;
    private final InetSocketAddress proxyAddress;
    private final InetSocketAddress proxyLocalAddress;
    private final long proxyId;

    public FrontendConnectInitHandler(ProxyConfig proxyConfig, InetSocketAddress proxyAddress, InetSocketAddress proxyLocalAddress, long proxyId) {
        super(proxyConfig.getProxyFrontendOptionConfig().getSslContext()
                , proxyConfig.getProxyFrontendOptionConfig().getNeedClientAuth(), false, proxyConfig.getExceptionFilter());
        this.proxyConfig = proxyConfig;
        this.proxyAddress = proxyAddress;
        this.proxyLocalAddress = proxyLocalAddress;
        this.proxyId = proxyId;
    }

    @Override
    public void onConnectReady(Channel channel) {
        if (logger.isDebugEnabled()) {
            logger.debug("onConnectReady {} {}", this.proxyId, ProxyConst.PROXY_TYPE_FRONTEND);
        }
        this.initBackendChannel(channel, channel.eventLoop(), proxyConfig, proxyAddress, proxyLocalAddress);
    }


    @Override
    public void onConnectError(Throwable throwable, Channel channel) {
        if (logger.isDebugEnabled()) {
            logger.debug("onConnectError {} {}", this.proxyId, ProxyConst.PROXY_TYPE_FRONTEND, throwable);
        }
        channel.close();
    }


    protected void initBackendChannel(final Channel frontendChannel, final EventLoop eventLoop, final ProxyConfig proxyConfig, final InetSocketAddress proxyAddress , final InetSocketAddress proxyLocalAddress) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoop)
                .channel(NioSocketChannel.class);
        final ProxyOptionConfig backendConfig = proxyConfig.getProxyBackendOptionConfig();
        ChannelConfigInitUtils.initBackendChannelConfig(bootstrap, backendConfig);
        bootstrap.option(ChannelOption.AUTO_READ, false);
        final SSLContext sslContext = proxyConfig.getProxyBackendOptionConfig().getSslContext();
        final Boolean needClientAuth = proxyConfig.getProxyBackendOptionConfig().getNeedClientAuth();
        bootstrap.handler(new ConnectActiveInitHandler(sslContext, needClientAuth, true, proxyConfig.getExceptionFilter()) {
            @Override
            public void onConnectReady(Channel backendChannel) {
                if (logger.isDebugEnabled()) {
                    logger.debug("onConnectReady {} {}", proxyId, ProxyConst.PROXY_TYPE_BACKEND);
                }
                ProxyHandler fProxyHandler = new ProxyHandler(proxyConfig.getKeepAliveFrontendMilliSecond(), frontendChannel, ProxyConst.PROXY_TYPE_FRONTEND, proxyId, proxyConfig.getExceptionFilter());
                ProxyHandler bProxyHandler = new ProxyHandler(proxyConfig.getKeepAliveBackendMilliSecond(), backendChannel, ProxyConst.PROXY_TYPE_BACKEND, proxyId, proxyConfig.getExceptionFilter());
                fProxyHandler.setRemoteProxyEventHandler(bProxyHandler);
                bProxyHandler.setRemoteProxyEventHandler(fProxyHandler);
                frontendChannel.pipeline().addLast(fProxyHandler);
                backendChannel.pipeline().addLast(bProxyHandler);
                FrontendConnectInitHandler.this.setProxyChannelReady();
                setProxyChannelReady();
                frontendChannel.config().setAutoRead(true);
                backendChannel.config().setAutoRead(true);
            }

            @Override
            public void onConnectError(Throwable throwable, Channel channel) {
                if (logger.isDebugEnabled()) {
                    logger.debug("onConnectError {} {}", proxyId, ProxyConst.PROXY_TYPE_BACKEND, throwable);
                }
                channel.close();
            }
        });
        if (proxyLocalAddress != null) {
            bootstrap.localAddress(proxyLocalAddress);
        }
        ChannelFuture channelFuture = bootstrap.connect(proxyAddress);
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                Channel backendChannel = future.channel();
                if (!future.isSuccess()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("{} error connect backend", proxyId, future.cause());
                    }
                    backendChannel.close();
                    frontendChannel.close();
                }
            }
        });
    }

}
