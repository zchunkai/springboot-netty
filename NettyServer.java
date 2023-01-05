package cn.platform.thinglinks.link.common.utils.netty;

import cn.platform.thinglinks.link.service.pcWorkHost.DeviceHostSystemInfoService;
import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Description netty服务端
 * @author zck
 * @date 2021/9/22
 */
@Slf4j
@Component
public class NettyServer {

    private Channel serverChannel = null;
    private EventLoopGroup bossGroup = null;
    private EventLoopGroup workGroup = null;
    private final int port = 12345;
    @Autowired
    private DeviceHostSystemInfoService infoService;

    public void start() throws InterruptedException {
        try {
            bossGroup = new NioEventLoopGroup();
            workGroup = new NioEventLoopGroup();
            ServerBootstrap sb = new ServerBootstrap();
            sb.option(ChannelOption.SO_RCVBUF,1024*20);
            sb.group(workGroup, bossGroup) // 绑定线程池
                    .channel(NioServerSocketChannel.class) // 指定使用的channel
                    .localAddress(this.port)// 绑定监听端口
                    .childOption(ChannelOption.SO_KEEPALIVE,true)
                    .childHandler(new ChannelInitializer<SocketChannel>() { // 绑定客户端连接时候触发操作
                        @Override
                        protected void initChannel(SocketChannel ch){
                            //解决拆包粘包
                            ByteBuf delimiter = Unpooled.copiedBuffer("\n".getBytes());
                            ch.config().setReceiveBufferSize(2048*20);
                            log.info("信息：有一客户端链接到本服务端");
                            log.info("IP:" + ch.localAddress().getHostName());
                            log.info("Port:" + ch.localAddress().getPort());
                            ch.pipeline()
                                    .addLast(new DelimiterBasedFrameDecoder(Integer.MAX_VALUE, delimiter))
                                    .addLast(new StringEncoder())
                                    .addLast(new EchoServerHandler(infoService)) // 客户端触发回调
                                    .addLast(new IdleStateHandlerInitializer()); // 客户端心跳
                        }
                    });
            // 服务器异步创建绑定
            ChannelFuture cf = sb.bind().sync();
            serverChannel=cf.channel();
            log.info(NettyServer.class + " netty服务端启动：" + cf.channel().localAddress());
            cf.channel().closeFuture().sync(); // 关闭服务器通道
        }
        catch (Exception e){
            log.error(e.getMessage());
        }
        finally {
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

    /**
     * 发送消息
     */
    public void sendMsg(String text) throws Exception{
        //每隔100秒向服务端发送一次心跳消息
        TimeUnit.SECONDS.sleep(10);
        serverChannel.writeAndFlush(Unpooled.copiedBuffer(JSON.toJSONString(text), CharsetUtil.UTF_8));
        log.info("服务端发送心跳消息:{}",text);
    }
    public static void main(String[] args) throws Exception {
//        new NettyServer(8888).start(); // 启动
    }




}
