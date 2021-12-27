package com.github.yiYangGit.tcpproxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannelConfig;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by yangyi on 2022/1/11.
 */
public class ChannelConfigInitUtils {


    private static final Logger logger = LoggerFactory.getLogger(ChannelConfigInitUtils.class);

    public static void initFrontendChannelConfig(final ProxyOptionConfig proxyOptionConfig, final NioSocketChannel child) {
        SocketChannelConfig config = child.config();
        config.setOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        config.setOption(ChannelOption.SO_LINGER, 3000);
        child.config().setMaxMessagesPerRead(16);
        Map<ChannelOption<Object>, Object> options = proxyOptionConfig.getOptions();
        for (Map.Entry<ChannelOption<Object>, Object> e : options.entrySet()) {
            try {
                if (!config.setOption((ChannelOption<Object>) e.getKey(), e.getValue())) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Unknown channel option: " + e);
                    }
                }
            } catch (Throwable t) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed to set a channel option: " + child, t);
                }
            }
        }
        child.config().setOption(ChannelOption.ALLOW_HALF_CLOSURE, false);
    }

    public static void initBackendChannelConfig(final Bootstrap bootstrap, final ProxyOptionConfig backendConfig) {
        bootstrap.option(ChannelOption.MAX_MESSAGES_PER_READ, 16);
        bootstrap.option(ChannelOption.SO_LINGER, 3000);
        for (Map.Entry<ChannelOption<Object>, Object> entry : backendConfig.getOptions().entrySet()) {
            bootstrap.option(entry.getKey(), entry.getValue());
        }
        //todo 优化半双工方法
        bootstrap.option(ChannelOption.ALLOW_HALF_CLOSURE, false);
    }
}
