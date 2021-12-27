
package com.github.yiYangGit.tcpproxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * netty 反向代理实现
 */
public class ProxyHandler extends ChannelInboundHandlerAdapter implements ProxyEventHandler{

    private static final Logger log = LoggerFactory.getLogger(ProxyHandler.class);

    private final Channel channel;
    private final String proxyType;
    private final long proxyId;
    private final ExceptionFilter exceptionFilter;
    private final long keepAliveOnRemoteClose;


    private ProxyEventHandler remoteProxyEventHandler;
    private DelayMustClose delayClose;
    private ScheduledFuture<?> delayCloneSchedule;
    private ChannelHandlerContext ctx;

    public void setRemoteProxyEventHandler(ProxyEventHandler remoteProxyEventHandler) {
        this.remoteProxyEventHandler = remoteProxyEventHandler;
    }

    public ProxyHandler(long keepAliveOnRemoteClose, Channel channel, String proxyType, long proxyId, ExceptionFilter exceptionFilter) {
        this.exceptionFilter = exceptionFilter;
        this.proxyType = proxyType;
        this.proxyId = proxyId;
        this.channel = channel;
        this.keepAliveOnRemoteClose = keepAliveOnRemoteClose;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        boolean writable = ctx.channel().isWritable();
        if (log.isDebugEnabled()) {
            log.debug("channelWritabilityChanged {} {} {}",proxyId,proxyType, writable);
        }
        if (writable) {
            this.remoteProxyEventHandler.onRemoteWriteStatusEnable();
        } else {
            this.remoteProxyEventHandler.onRemoteWriteStatusDisable();
        }
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("channelRead {} {} {}", proxyId, proxyType, ((ByteBuf) msg).readableBytes());
        }
        this.remoteProxyEventHandler.onRemoteRead(msg);
    }

    @Override
    public void onRemoteReadComplete() {
        if (log.isDebugEnabled()) {
            log.debug("flush {} {}", proxyId, proxyType);
        }
        this.ctx.flush();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        this.remoteProxyEventHandler.onRemoteReadComplete();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("channelInactive {} {}", proxyId, proxyType);
        }
        fastCleanResources();
        this.remoteProxyEventHandler.onRemoteConnectClose();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        if (this.exceptionFilter != null && this.exceptionFilter.isIgnoreException(cause)) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("exceptionCaught {} {}", proxyId, proxyType);
        }
        this.channel.close();
    }

    @Override
    public void onRemoteRead(Object byteBuf) {
        if (this.channel.isActive()) {
            this.ctx.write(byteBuf).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        } else {
            ReferenceCountUtil.release(byteBuf);
        }
    }

    /**
     * 当前连接关闭时快速关闭连接
     */
    public void fastCleanResources() {
        if (this.delayClose != null) {
            this.delayClose.clearChannel();
        }
        if (this.delayCloneSchedule != null) {
            this.delayCloneSchedule.cancel(false);
        }
    }

    @Override
    public void onRemoteConnectClose() {
        if (this.channel.isActive()) {
            ChannelFuture flush = this.channel.pipeline().writeAndFlush(Unpooled.EMPTY_BUFFER);
            long keepAliveOnRemoteClose = this.keepAliveOnRemoteClose;
            if (keepAliveOnRemoteClose > 0) {
                flush.addListener(ChannelFutureListener.CLOSE);
                this.delayClose = new DelayMustClose(this.channel);
                this.delayCloneSchedule = this.channel.eventLoop().schedule(delayClose, keepAliveOnRemoteClose, TimeUnit.MILLISECONDS);
            } else {
                this.channel.close();
            }
        }
    }

    @Override
    public void onRemoteWriteStatusDisable() {
        if (this.channel.isActive()) {
            this.channel.config().setAutoRead(false);
        }
    }

    @Override
    public void onRemoteWriteStatusEnable() {
        if (this.channel.isActive()) {
            this.channel.config().setAutoRead(true);
        }
    }

}
