package com.tiamo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tiamo.entity.Friend;
import com.tiamo.entity.FriendRequest;

import java.util.List;
import java.util.Map;

/**
 * 好友服务接口
 */
public interface FriendService extends IService<Friend> {

    /**
     * 发送好友请求
     */
    Map<String, Object> sendFriendRequest(Long fromUserId, String fromUsername, String toUsername, String message);

    /**
     * 接受好友请求
     */
    Map<String, Object> acceptFriendRequest(Long requestId, Long userId);

    /**
     * 拒绝好友请求
     */
    Map<String, Object> rejectFriendRequest(Long requestId, Long userId);

    /**
     * 获取好友列表
     */
    List<Map<String, Object>> getFriendList(Long userId);

    /**
     * 获取收到的好友请求列表
     */
    List<FriendRequest> getReceivedRequests(Long userId);

    /**
     * 获取发送的好友请求列表
     */
    List<FriendRequest> getSentRequests(Long userId);

    /**
     * 获取待处理请求数量
     */
    int getPendingRequestCount(Long userId);

    /**
     * 删除好友
     */
    Map<String, Object> deleteFriend(Long userId, Long friendId);

    /**
     * 检查是否是好友
     */
    boolean isFriend(Long userId, Long friendId);
}
