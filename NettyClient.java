package cn.platform.thinglinks.pcWorkHost.utils;

import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;

import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyClient {

    private ChannelFuture channelFuture;
    private Bootstrap bootstrap;

    private Channel channel;

    private static final String IP = "192.168.1.42";
//    private static final String IP = "127.0.0.1";

    private static final Integer PORT = 12345;
    public void start(){
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            //创建bootstrap对象，配置参数
            bootstrap = new Bootstrap();
            //设置线程组
            bootstrap.group(group)
                    //设置客户端的通道实现类型
                    .channel(NioSocketChannel.class)
                    //使用匿名内部类初始化通道
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            //添加客户端通道的处理器
                            ch.pipeline()
                                    .addLast(new NettyClientHandler())
                                    .addLast("decoder",new StringDecoder())
                                    .addLast("encoder",new StringEncoder());
                        }
                    });
            //连接服务端
            channelFuture = bootstrap.connect(IP,PORT).sync();
            channel = channelFuture.channel();
            log.info("连接成功!");
            //无限次向服务端发送心跳请求
            while (channel.isActive()){
                sendMsg("ping");
            }
            //对通道关闭进行监听
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            //关闭线程组
            group.shutdownGracefully();
            try {
                //掉线时30秒后重新启动客户端
                TimeUnit.SECONDS.sleep(5);
                log.info("重新启动客户端.........");
                start();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
    public void sendMsg(String text) throws Exception{
        //每隔100秒向服务端发送一次心跳消息
        TimeUnit.SECONDS.sleep(100);
        channel.writeAndFlush(Unpooled.copiedBuffer(JSON.toJSONString(text)+"\n", CharsetUtil.UTF_8));
        log.info("客户端发送心跳消息:{}",text);
    }
}
