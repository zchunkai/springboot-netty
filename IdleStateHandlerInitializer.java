package cn.platform.thinglinks.link.common.utils.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import java.util.concurrent.TimeUnit;
/**
* @description 服务端检测连接
* @methodName IdleStateHandlerInitializer
* @author zck
* @date 2023/1/4 11:15
* @param null
* @return
**/
public class IdleStateHandlerInitializer extends ChannelInitializer<Channel> {
    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new IdleStateHandler(0, 0, 20, TimeUnit.SECONDS));// IdleStateHandler将在被触发时发送一个IdleStateEvent事件
        pipeline.addLast(new HeartbeatHandler());// 将一个HeartbeatHandler添加到ChannelPipeline中
    }

    public static final class HeartbeatHandler extends ChannelInboundHandlerAdapter {// 实现userEventTriggered()方法以发送心跳消息
        private static final ByteBuf HEARTBEAT_SEQUENCE = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("HEARTBEAT", CharsetUtil.ISO_8859_1));// 发送到远程节点的心跳消息
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx,Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {// 发送心跳消息，并在发送失败时关闭该连接
                ctx.writeAndFlush(HEARTBEAT_SEQUENCE.duplicate()).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            } else {
                super.userEventTriggered(ctx, evt);// 不是IdleStateEvent事件，所以将它传递给下一个ChannelInboundHandler
            }
        }
    }
}
