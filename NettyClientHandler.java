package cn.platform.thinglinks.pcWorkHost.utils;

import cn.platform.thinglinks.pcWorkHost.domain.SystemInfo;
import cn.platform.thinglinks.pcWorkHost.domain.SystemProcessInfo;
import cn.platform.thinglinks.pcWorkHost.domain.WorkHostMsg;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import io.netty.channel.ChannelHandler.Sharable;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * @author zck
 */
@Slf4j
@Sharable
public class NettyClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    public void channelActive(ChannelHandlerContext ctx){
        //发送消息到服务端
        try {
            WorkHostMsg workHostMsg = new WorkHostMsg();
            workHostMsg.setMac(WorkHostUtils.getMac());
            ByteBuf msg = getMsg(workHostMsg);
            ctx.channel().writeAndFlush(msg);
            log.info("服务端发送连接信息:{}",JSON.toJSONString(workHostMsg));
            //存储连接信息
            ChannelMap.addChannel(workHostMsg.getMac(),ctx.channel());
        }catch (Exception e){
            e.getMessage();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext cxt, ByteBuf byteBuf) throws Exception {
        // 第一种：接收字符串时的处理
        try {
            String message = byteBuf.toString(CharsetUtil.UTF_8);
            if (message.indexOf("mac")>0){
                WorkHostMsg hostMsg = JSONObject.parseObject(message,WorkHostMsg.class);
                log.info("客户端收到数据:"+hostMsg);
                if (hostMsg!=null){
                    if (hostMsg.getType()!=null){
                        switch (hostMsg.getType()) {
                            /**关机**/
                            case 1:
                                WorkHostUtils.shutdown(hostMsg);
                                break;
                            /**重启**/
                            case 2:
                                WorkHostUtils.restart(hostMsg);
                                break;
                            /**休眠**/
                            case 3:
                                if (WorkHostUtils.dormancy(hostMsg)){
                                    hostMsg.setStatus(true);
                                }
                                break;
                            /**获取系统信息**/
                            case 4:
//                                SystemInfo systemMsg = WorkHostUtils.getSystemMsg(hostMsg);
                                SystemInfo systemMsg = OshiUtils.getSystemMsg(hostMsg);
                                hostMsg.setInfo(systemMsg);
                                cxt.channel().writeAndFlush(getMsg(hostMsg));
                                log.info("服务端发送系统信息:{}",JSON.toJSONString(hostMsg));
                                break;
                            /**获取系统进程信息***/
                            case 5:
                                List<SystemProcessInfo> list = WorkHostUtils.getSystemProcessMsg(hostMsg);
                                if (!list.isEmpty()){
                                    hostMsg.setProcessInfos(list);
                                    cxt.channel().writeAndFlush(getMsg(hostMsg));
                                    log.info("服务端发送系统进程信息:{}",JSON.toJSONString(hostMsg));
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }catch (Exception e){
            log.error("接收数据异常!");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("连接出现异常!");
        cause.getCause();
        log.error(cause.getMessage());
        //清除连接
        ChannelMap.remoteChannel();
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.error("掉线了...");
        super.channelInactive(ctx);
        //清除连接
        ChannelMap.remoteChannel();
    }

    /**
     * @param buf
     * @return
     * @author Taowd
     * TODO  此处用来处理收到的数据中含有中文的时  出现乱码的问题
     * 2017年8月31日 下午7:57:28
     */
    private String getMessage(ByteBuf buf) {
        byte[] con = new byte[buf.readableBytes()];
        buf.readBytes(con);
        try {
            return new String(con, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ByteBuf getMsg(Object obj){
        return Unpooled.copiedBuffer(JSON.toJSON(obj).toString()+"\n",CharsetUtil.UTF_8);
    }
}
