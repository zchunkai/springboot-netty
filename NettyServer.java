package com.vko.win.utils;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;

/**
 * Description netty服务端
 * @author zck
 * @date 2021/9/22
 */
@Slf4j
public class NettyServer {

    private static Channel serverChannel = null;
    private static EventLoopGroup bossGroup = null;
    private static EventLoopGroup workGroup = null;
    private final int port;

    public NettyServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        try {
            bossGroup = new NioEventLoopGroup();
            workGroup = new NioEventLoopGroup();
            ServerBootstrap sb = new ServerBootstrap();
            sb.option(ChannelOption.SO_BACKLOG, 1024);
            sb.group(workGroup, bossGroup) // 绑定线程池
                    .channel(NioServerSocketChannel.class) // 指定使用的channel
                    .localAddress(this.port)// 绑定监听端口
                    .childHandler(new ChannelInitializer<SocketChannel>() { // 绑定客户端连接时候触发操作

                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            log.info("信息：有一客户端链接到本服务端");
                            log.info("IP:" + ch.localAddress().getHostName());
                            log.info("Port:" + ch.localAddress().getPort());

                            ch.pipeline().addLast(new StringEncoder(Charset.forName("GBK")));
                            ch.pipeline().addLast(new EchoServerHandler()); // 客户端触发操作
                            ch.pipeline().addLast(new ByteArrayEncoder());
                        }
                    });
            ChannelFuture cf = sb.bind().sync(); // 服务器异步创建绑定
            serverChannel=cf.channel();
            log.info(NettyServer.class + " netty服务端启动：" + cf.channel().localAddress());
            cf.channel().closeFuture().sync(); // 关闭服务器通道
        } finally {
            workGroup.shutdownGracefully().sync(); // 释放线程池资源
            bossGroup.shutdownGracefully().sync();
        }
    }



    /**
     * 关闭netty server
     */
    public void close() {
        if (serverChannel != null) {
            try {
                serverChannel.close();
                serverChannel = null;
                bossGroup.shutdownGracefully();
                workGroup.shutdownGracefully().sync();
            } catch (Exception e) {
                log.error("Close netty server failed.");
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args) throws Exception {

        new NettyServer(8888).start(); // 启动
    }




}
