package com.github.yiYangGit.tcpproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by yangyi on 2021/12/23.
 */
public class ConnectEndpoint {

    private static final Logger log = LoggerFactory.getLogger(ConnectEndpoint.class);

    private final int maxConnect;
    public static final int NOT_LIMIT_NUMBER = 0;
    private final Semaphore connectionCounter;
    private final SocketAddress bingAddress;
    private EndpointStatus status;
    private Thread acceptThread;
    private ServerSocketChannel serverSocket;

    public ConnectEndpoint(int maxConnect, SocketAddress address) {
        this.maxConnect = maxConnect;
        this.bingAddress = address;
        this.connectionCounter = new Semaphore(maxConnect);
        this.status = EndpointStatus.NOT_START;
    }

    public synchronized void start(ServerSocketConfig socketConfig, final SocketAcceptListener socketAcceptListener) throws Exception {
        if (status != EndpointStatus.NOT_START) {
            throw new IllegalStateException("status " + status.name() + " unSupport start !!!");
        }
        status = EndpointStatus.RUNNING;
        serverSocket = ServerSocketChannel.open();
        int backlog = ServerSocketConfig.DEFAULT_BACKLOG;
        if (socketConfig != null) {
            Integer rxBufSize = socketConfig.getRxBufSize();
            if (rxBufSize != null) {
                serverSocket.socket().setReceiveBufferSize(rxBufSize);
            }
            Boolean reuseAddress = socketConfig.getSoReuseAddress();
            if (reuseAddress != null) {
                serverSocket.socket().setReuseAddress(reuseAddress);
            }
            backlog = socketConfig.getServerBacklog();
        }
        //1秒阻塞方式的acceptSocket
        int soTimeout = 1000;
        serverSocket.socket().setSoTimeout(soTimeout);
        serverSocket.configureBlocking(true);
        serverSocket.bind(bingAddress, backlog);
        //copyright 此处代码参考apache tomcat 项目的 org.apache.tomcat.util.net.NioEndpoint.Acceptor#run 方法
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                int errorDelay = 0;

                while (status == EndpointStatus.RUNNING) {
                    try {
                        try {
                            if (maxConnect > NOT_LIMIT_NUMBER) {
                                boolean isAccept = connectionCounter.tryAcquire(1, TimeUnit.SECONDS);
                                if (!isAccept) {
                                    continue;
                                }
                            }
                        } catch (InterruptedException continueRun) {
                        }

                        SocketChannel socket = null;
                        try {
                            socket = serverSocket.accept();
                        } catch (IOException ioe) {
                            connectionCounter.release();
                            // Introduce delay if necessary
                            errorDelay = handleExceptionWithDelay(errorDelay);
                            // re-throw
                            throw ioe;
                        }
                        errorDelay = 0;

                        if (status == EndpointStatus.RUNNING) {
                            try {
                                socketAcceptListener.onAcceptSocketChannel(socket);
                            } catch (Exception notExpect) {
                                closeSocket(socket);
                                connectionCounter.release();
                            }
                        } else {
                            connectionCounter.release();
                            closeSocket(socket);
                        }
                    } catch (SocketTimeoutException sx) {
                        //ignore
                    } catch (IOException ioException) {
                        if (log.isErrorEnabled() && status== EndpointStatus.RUNNING) {
                            log.error("accept error", ioException);
                        }
                    } catch (OutOfMemoryError oom) {
                        //oom 时直接退出,本系统的代码不兼容 OOM 异常
                        log.error("", oom);
                        return;
                    }
                }
                status = EndpointStatus.STOP;
            }
        });
        thread.setName(ConnectEndpoint.class.getSimpleName() + " -- Thead");
        acceptThread = thread;
        thread.start();
    }

    /**
     * 关闭接收器
     */
    public synchronized void stop() {
        if (this.status != EndpointStatus.STOP && this.status != EndpointStatus.STOPPING) {
            this.status = EndpointStatus.STOPPING;
            if (this.acceptThread != null) {
                this.acceptThread.interrupt();
            }
        }
    }
    private void closeSocket(SocketChannel socket) {
        try {
            socket.socket().close();
        } catch (IOException ioe)  {
            if (log.isDebugEnabled()) {
                log.debug("", ioe);
            }
        }
        try {
            socket.close();
        } catch (IOException ioe) {
            if (log.isDebugEnabled()) {
                log.debug("", ioe);
            }
        }
    }

    private int handleExceptionWithDelay(int currentErrorDelay) {
        if (currentErrorDelay > 0) {
            try {
                Thread.sleep(currentErrorDelay);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        int maxDelay = 1600;
        int initDelay = 50;
        if (currentErrorDelay == 0) {
            return initDelay;
        } else {
            if (currentErrorDelay < maxDelay) {
                return currentErrorDelay * 2;
            } else {
                return maxDelay;
            }
        }
    }


    /**
     * 释放一个连接,所有 this.start
     */
    public void releaseConnection() {
        if (maxConnect > 0) {
            connectionCounter.release();
        }
    }

    public interface SocketAcceptListener {
        public void onAcceptSocketChannel(SocketChannel socketChannel);
    }
}

enum EndpointStatus {
    NOT_START, RUNNING,STOPPING, STOP
}


