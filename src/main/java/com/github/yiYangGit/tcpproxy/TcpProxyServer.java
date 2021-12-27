package com.github.yiYangGit.tcpproxy;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicLong;


public class TcpProxyServer {

    private static final Logger logger = LoggerFactory.getLogger(TcpProxyServer.class);
    private EventLoopGroup workerGroup;
    private ConnectEndpoint endpoint;
    private AtomicLong proxyCounter = new AtomicLong();

    public void run(InetSocketAddress serverAddress, final InetSocketAddress proxyAddress ,final InetSocketAddress proxyLocalAddress, final ProxyConfig proxyConfig) throws Exception {
        this.workerGroup = new NioEventLoopGroup(proxyConfig.getIoThreads());
        this.endpoint = new ConnectEndpoint(proxyConfig.getMaxConnects(), serverAddress);
        endpoint.start(proxyConfig.getServerSocketConfig(), new ConnectEndpoint.SocketAcceptListener() {
            @Override
            public void onAcceptSocketChannel(final SocketChannel socketChannel) {
                workerGroup.execute(new Runnable() {
                    @Override
                    public void run() {
                        registerChannel(socketChannel, proxyConfig, proxyAddress,proxyLocalAddress);
                    }
                });
            }
        });
        if (logger.isDebugEnabled()) {
            logger.debug("start proxy server");
        }
    }

    private void registerChannel(final SocketChannel socketChannel, final ProxyConfig proxyConfig, final InetSocketAddress proxyAddress, final InetSocketAddress proxyLocalAddress) {
        long proxyId = proxyCounter.incrementAndGet();
        NioSocketChannel child = new NioSocketChannel(socketChannel);
        final ProxyOptionConfig fOptionConfig = proxyConfig.getProxyFrontendOptionConfig();
        ChannelConfigInitUtils.initFrontendChannelConfig(fOptionConfig, child);
        child.config().setOption(ChannelOption.AUTO_READ, false);
        child.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                endpoint.releaseConnection();
            }
        });
        child.pipeline().addLast(new FrontendConnectInitHandler(proxyConfig, proxyAddress, proxyLocalAddress, proxyId));
        workerGroup.register(child).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }

    public void close() {
        if (this.endpoint != null) {
            this.endpoint.stop();
        }
        if (this.workerGroup != null) {
            this.workerGroup.shutdownGracefully();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("close proxy server");
        }
    }

}
