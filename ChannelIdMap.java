package cn.platform.thinglinks.link.common.utils.netty;


import io.netty.channel.ChannelId;

import java.util.concurrent.ConcurrentHashMap;
/**
* @description 存储channelId与mac地址
* @className ChannelIdMap
* @author zck
*@date 2022/12/26 9:28
**/
public class ChannelIdMap {
    /**
     * 存放客户端标识ID（消息ID）与channel的对应关系
     */
    private static volatile ConcurrentHashMap<ChannelId, String> channelMap = null;

    private ChannelIdMap() {
    }

    public static ConcurrentHashMap<ChannelId, String> getChannelMap() {
        if (null == channelMap) {
            synchronized (ChannelMap.class) {
                if (null == channelMap) {
                    channelMap = new ConcurrentHashMap<>();
                }
            }
        }
        return channelMap;
    }

    public static String getChannel(ChannelId id) {
        return getChannelMap().get(id);
    }

    public static void addChannel(ChannelId id,String mac){
        getChannelMap().put(id,mac);
    }

    public static void remove(ChannelId id){
        getChannelMap().remove(id);
    }



}
