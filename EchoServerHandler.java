package cn.platform.thinglinks.link.common.utils.netty;


import cn.platform.thinglinks.common.core.constant.LinkConstants;
import cn.platform.thinglinks.common.core.utils.StringUtils;
import cn.platform.thinglinks.link.api.domain.pcWorkHost.DeviceHostSystemInfo;
import cn.platform.thinglinks.link.api.domain.pcWorkHost.WorkHostMsg;
import cn.platform.thinglinks.link.service.pcWorkHost.DeviceHostSystemInfoService;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import io.netty.channel.ChannelHandler.Sharable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author zck
 */
@Slf4j
@Sharable
@Component
public class EchoServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Autowired
    private DeviceHostSystemInfoService infoService;

    public EchoServerHandler(DeviceHostSystemInfoService infoService){
        this.infoService=infoService;
    }
    /**
     * channelAction
     * channel 通道 action 活跃的
     * 当客户端主动链接服务端的链接后，这个通道就是活跃的了。也就是客户端与服务端建立了通信通道并且可以传输数据
     */
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info(ctx.channel().localAddress().toString() + "客户端已连接!");
    }

    /**
     * channelInactive
     * channel 通道 Inactive 不活跃的
     * 当客户端主动断开服务端的链接后，这个通道就是不活跃的。也就是说客户端与服务端的关闭了通信通道并且不可以传输数据
     */
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info(ctx.channel().localAddress().toString() + "客户端已断开!");
        //清除连接信息
        log.info("删除netty连接{}",remoteConnect(ctx)?"成功":"失败");
    }


    /**
     * 功能:接收客户端数据
     * **/
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf){
        try {
            String message = byteBuf.toString(CharsetUtil.UTF_8);
            if (message.indexOf("mac")>0){
                log.info("服务端收到数据:"+message);
                if (!writeData(ctx,message)){
                    throw new Exception();
                }
            }else if (message.indexOf("ping")>0){
                //心跳 不处理
            }
        }catch (Exception e){
            e.printStackTrace();
            log.error("客户端发送消息接收失败!");
        }
    }

    /**
     * 功能：读取完毕客户端发送过来的数据之后的操作
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        String mac = ChannelIdMap.getChannel(ctx.channel().id());
        log.info("服务端到接收来自{}发送的数据",mac);
        // 第一种方法：写一个空的buf，并刷新写出区域。完成后关闭sock channel连接。
//        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        // 第二种方法：在client端关闭channel连接，这样的话，会触发两次channelReadComplete方法。
        // ctx.flush().close().sync(); // 第三种：改成这种写法也可以，但是这中写法，没有第一种方法的好。
    }

    /**
     * 功能：服务端发生异常的操作
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        //触发channelInactive
        ctx.close();
        log.info("连接出现异常!");
        log.error(cause.getMessage());
        //清除连接信息
//        log.info("删除netty连接{}",remoteConnect(ctx)?"成功":"失败");
    }

    /**
     * 功能:清除连接信息
     * **/
    public Boolean remoteConnect(ChannelHandlerContext ctx){
        try {
            String mac = ChannelIdMap.getChannel(ctx.channel().id());
            if (StringUtils.isNotNull(mac)){
                ChannelIdMap.remove(ctx.channel().id());
                if (StringUtils.isNotNull(ChannelMap.getChannel(mac))){
                    ChannelMap.remove(mac);
                    //修改设备在线状态
                    infoService.updateDeviceType(mac, LinkConstants.CONNECTOR_STATUS_OFFLINE);
                    //添加关机日志
                    infoService.updateDeviceHostRunLog(mac);
                }
            }
            return true;
        }catch (Exception e){
            return false;
        }
    }

    /**
     * 新增或更新连接信息
     * ***/
    public void updateConnect(ChannelHandlerContext ctx,WorkHostMsg hostMsg){
        try {
            if (StringUtils.isNotNull(hostMsg)) {
                //更新连接
                if (StringUtils.isNull(ChannelIdMap.getChannel(ctx.channel().id()))) {
                    ChannelIdMap.addChannel(ctx.channel().id(), hostMsg.getMac());
                }
                if (StringUtils.isNull(ChannelMap.getChannel(hostMsg.getMac()))) {
                    ChannelMap.addChannel(hostMsg.getMac(), ctx);
                    //修改设备在线状态
                    infoService.updateDeviceType(hostMsg.getMac(), LinkConstants.CONNECTOR_STATUS_ONLINE);
                    //添加开机日志
                    infoService.addDeviceHostRunLog(hostMsg.getMac());
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 功能:处理客户端数据
     * **/
    public Boolean writeData(ChannelHandlerContext ctx, String msg){
        try {
            WorkHostMsg hostMsg = JSONObject.parseObject(msg,WorkHostMsg.class);
            if (StringUtils.isNotNull(hostMsg)){
                //更新连接
                updateConnect(ctx,hostMsg);

                //解析数据
                if (StringUtils.isNotNull(hostMsg.getType())){
                    switch (hostMsg.getType()) {
                        /**系统信息***/
                        case 4:
                            if (StringUtils.isNotNull(hostMsg.getInfo())){
                                DeviceHostSystemInfo info = new DeviceHostSystemInfo(hostMsg.getInfo());
                                info.setMac(hostMsg.getMac());
                                log.info("收到系统信息:{}",hostMsg);
                                infoService.insert(info);
                            }
                            break;
                        /**进程信息***/
                        case 5:
                            if (StringUtils.isNotNull(hostMsg.getProcessInfos())){
                                infoService.insertProcessInfos(hostMsg);
                                log.info("收到进程信息:{}",hostMsg);
                            }
                            break;
                        default:
                            break;
                    }
                }

            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }
}
