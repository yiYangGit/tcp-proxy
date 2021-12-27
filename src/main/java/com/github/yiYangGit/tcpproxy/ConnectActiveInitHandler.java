package com.github.yiYangGit.tcpproxy;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.*;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.util.LinkedList;

/**
 * Created by yangyi on 2021/12/29.
 */
public abstract class ConnectActiveInitHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ConnectActiveInitHandler.class);

    private SSLContext sslContext;
    private Boolean needClientAuth;
    private boolean isClient;
    private LinkedList<Object> cacheBuffers = new LinkedList<>();
    private ExceptionFilter exceptionFilter;
    private boolean proxyChannelIsReady = false;
    private ChannelHandlerContext ctx;

    public void setProxyChannelReady() {
        this.proxyChannelIsReady = true;
        if (!this.cacheBuffers.isEmpty()) {
            for (Object cacheBuffer : cacheBuffers) {
                this.ctx.fireChannelRead(cacheBuffer);
            }
            this.ctx.fireChannelReadComplete();
        }
        cacheBuffers.clear();
    }

    public ConnectActiveInitHandler(SSLContext sslContext, Boolean needClientAuth, boolean isClient, ExceptionFilter exceptionFilter) {
        this.sslContext = sslContext;
        this.needClientAuth = needClientAuth;
        this.isClient = isClient;
        this.exceptionFilter = exceptionFilter;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        Channel channel = ctx.channel();
        if (sslContext != null) {
            ClientAuth authType = ClientAuth.NONE;
            if (!isClient && needClientAuth != null) {
                if (needClientAuth.equals(Boolean.TRUE)) {
                    authType = ClientAuth.REQUIRE;
                }
            }
            JdkSslContext jdkSslContext = new JdkSslContext(sslContext, isClient, authType);
            SslHandler sslHandler = new SslHandler(jdkSslContext.newEngine(channel.alloc()), !isClient);
            channel.pipeline().addFirst(sslHandler);
            channel.config().setAutoRead(true);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (this.sslContext == null) {
            this.doConnectReady(ctx.channel());
        }
    }

    protected void doConnectReady(Channel channel) {
        this.onConnectReady(channel);
    }

    public abstract void onConnectReady(Channel channel);

    public abstract void onConnectError(Throwable throwable, Channel channel);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (this.proxyChannelIsReady) {
            ctx.fireChannelRead(msg);
        }else {
            this.cacheBuffers.add(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (!this.cacheBuffers.isEmpty()) {
            for (Object cacheBuffer : this.cacheBuffers) {
                ReferenceCountUtil.release(cacheBuffer);
            }
        }
        cacheBuffers.clear();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof SslHandshakeCompletionEvent) {
            if (log.isDebugEnabled()) {
                log.debug("userEventTriggered {}", evt);
            }
            SslHandshakeCompletionEvent shce = (SslHandshakeCompletionEvent) evt;
            if (shce.isSuccess()) {
                Channel channel = ctx.channel();
                if (log.isDebugEnabled()) {
                    log.debug("userEventTriggered setAutoRead false {}", evt);
                }
                channel.config().setAutoRead(false);
                ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
                this.doConnectReady(channel);
            } else {
                if (this.exceptionFilter != null && this.exceptionFilter.isIgnoreHandshakeEvent(shce)) {
                    return;
                }
                this.onConnectError(shce.cause(), ctx.channel());
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (this.exceptionFilter != null && this.exceptionFilter.isIgnoreException(cause)) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("exceptionCaught ", cause);
        }
        this.onConnectError(cause, ctx.channel());
    }

}
