package com.tiamo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tiamo.entity.ChatMessage;

import java.util.List;
import java.util.Map;

/**
 * 聊天服务接口
 */
public interface ChatService extends IService<ChatMessage> {

    /**
     * 发送消息
     */
    Map<String, Object> sendMessage(Long fromUserId, String fromUsername, Long toUserId, String content, String msgType);

    /**
     * 获取与某个好友的聊天记录
     */
    List<ChatMessage> getChatHistory(Long userId, Long friendId, int page, int size);

    /**
     * 获取会话列表（最近聊天的好友）
     */
    List<Map<String, Object>> getConversationList(Long userId);

    /**
     * 获取未读消息数量
     */
    int getUnreadCount(Long userId);

    /**
     * 获取与某个好友的未读消息数量
     */
    int getUnreadCountWithFriend(Long userId, Long friendId);

    /**
     * 标记与某个好友的消息为已读
     */
    Map<String, Object> markAsRead(Long userId, Long friendId);

    /**
     * 获取新消息（轮询用）
     */
    List<ChatMessage> getNewMessages(Long userId, Long lastMessageId);
}
