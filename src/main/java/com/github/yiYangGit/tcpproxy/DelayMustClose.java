package com.github.yiYangGit.tcpproxy;

import io.netty.channel.Channel;

/**
 * Created by yangyi on 2021/12/24.
 */
public class DelayMustClose implements Runnable {


    private Channel channel;

    /**
     *fast gc
     * */
    public void clearChannel() {
        this.channel = null;
    }

    public DelayMustClose(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void run() {
        Channel channel = this.channel;
        if (channel != null) {
            channel.close();
        }
    }
}
