package cn.platform.thinglinks.link.common.utils.netty;

import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ConcurrentHashMap;

public class ChannelMap {
    /**
     * 存放客户端标识ID（消息ID）与channel的对应关系
     */
    private static volatile ConcurrentHashMap<String, ChannelHandlerContext> channelMap = null;

    private ChannelMap() {
    }

    public static ConcurrentHashMap<String, ChannelHandlerContext> getChannelMap() {
        if (null == channelMap) {
            synchronized (ChannelMap.class) {
                if (null == channelMap) {
                    channelMap = new ConcurrentHashMap<>();
                }
            }
        }
        return channelMap;
    }

    public static ChannelHandlerContext getChannel(String id) {
        return getChannelMap().get(id);
    }

    public static void addChannel(String id,ChannelHandlerContext channel){
        getChannelMap().put(id,channel);
    }

    public static void remove(String id){
        getChannelMap().remove(id);
    }
}
