package com.github.yiYangGit.tcpproxy;

import io.netty.handler.ssl.SslHandshakeCompletionEvent;

/**
 * 处理要忽略的 ssl握手事件
 * Created by yangyi on 2022/1/10.
 */
public interface ExceptionFilter {

    /**
     * @param event
     * @return
     */
    boolean isIgnoreHandshakeEvent(SslHandshakeCompletionEvent event);

    /**
     * 是否忽略运行时发生的某些异常
     * @param throwable
     * @return
     */
    boolean isIgnoreException(Throwable throwable);

}
