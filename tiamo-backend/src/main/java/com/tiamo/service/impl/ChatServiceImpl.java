package com.tiamo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tiamo.entity.ChatMessage;
import com.tiamo.entity.Friend;
import com.tiamo.mapper.ChatMessageMapper;
import com.tiamo.mapper.FriendMapper;
import com.tiamo.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天服务实现
 */
@Service
public class ChatServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatService {

    @Autowired
    private FriendMapper friendMapper;

    /**
     * 应用启动时自动建表
     */
    @PostConstruct
    public void init() {
        try {
            baseMapper.createTableIfNotExists();
            System.out.println("[聊天系统] 数据库表初始化完成");
        } catch (Exception e) {
            System.out.println("[聊天系统] 数据库表初始化失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> sendMessage(Long fromUserId, String fromUsername, Long toUserId, String content, String msgType) {
        Map<String, Object> result = new HashMap<>();

        if (content == null || content.trim().isEmpty()) {
            result.put("success", false);
            result.put("msg", "消息内容不能为空");
            return result;
        }

        // 检查是否是好友
        LambdaQueryWrapper<Friend> friendWrapper = new LambdaQueryWrapper<>();
        friendWrapper.eq(Friend::getUserId, fromUserId).eq(Friend::getFriendId, toUserId);
        if (friendMapper.selectCount(friendWrapper) == 0) {
            result.put("success", false);
            result.put("msg", "只能给好友发送消息");
            return result;
        }

        // 获取接收者用户名
        LambdaQueryWrapper<Friend> toFriendWrapper = new LambdaQueryWrapper<>();
        toFriendWrapper.eq(Friend::getUserId, toUserId).eq(Friend::getFriendId, fromUserId);
        Friend toFriend = friendMapper.selectOne(toFriendWrapper);
        String toUsername = toFriend != null ? toFriend.getFriendUsername() : "";

        // 创建消息
        ChatMessage message = new ChatMessage();
        message.setFromUserId(fromUserId);
        message.setFromUsername(fromUsername);
        message.setToUserId(toUserId);
        message.setToUsername(toUsername);
        message.setContent(content.trim());
        message.setMsgType(msgType != null ? msgType : "text");
        message.setIsRead(0);
        message.setCreateTime(LocalDateTime.now());
        baseMapper.insert(message);

        result.put("success", true);
        result.put("msg", "发送成功");
        result.put("messageId", message.getId());
        result.put("createTime", message.getCreateTime());
        return result;
    }

    @Override
    public List<ChatMessage> getChatHistory(Long userId, Long friendId, int page, int size) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(ChatMessage::getFromUserId, userId).eq(ChatMessage::getToUserId, friendId)
                        .or().eq(ChatMessage::getFromUserId, friendId).eq(ChatMessage::getToUserId, userId))
                .orderByDesc(ChatMessage::getId);

        Page<ChatMessage> pageResult = baseMapper.selectPage(new Page<>(page, size), wrapper);
        List<ChatMessage> records = pageResult.getRecords();
        // 按时间正序排列
        records.sort(Comparator.comparing(ChatMessage::getId));
        return records;
    }

    @Override
    public List<Map<String, Object>> getConversationList(Long userId) {
        // 获取所有好友
        LambdaQueryWrapper<Friend> friendWrapper = new LambdaQueryWrapper<>();
        friendWrapper.eq(Friend::getUserId, userId);
        List<Friend> friends = friendMapper.selectList(friendWrapper);

        List<Map<String, Object>> conversations = new ArrayList<>();
        for (Friend friend : friends) {
            Map<String, Object> conv = new HashMap<>();
            conv.put("friendId", friend.getFriendId());
            conv.put("friendUsername", friend.getFriendUsername());
            conv.put("friendNickname", friend.getFriendNickname());

            // 获取最近一条消息
            LambdaQueryWrapper<ChatMessage> msgWrapper = new LambdaQueryWrapper<>();
            msgWrapper.and(w -> w.eq(ChatMessage::getFromUserId, userId).eq(ChatMessage::getToUserId, friend.getFriendId())
                            .or().eq(ChatMessage::getFromUserId, friend.getFriendId()).eq(ChatMessage::getToUserId, userId))
                    .orderByDesc(ChatMessage::getId).last("LIMIT 1");
            ChatMessage lastMsg = baseMapper.selectOne(msgWrapper);

            if (lastMsg != null) {
                conv.put("lastMessage", lastMsg.getContent());
                conv.put("lastMessageTime", lastMsg.getCreateTime());
                conv.put("lastMessageId", lastMsg.getId());
                conv.put("lastFromUserId", lastMsg.getFromUserId());
            } else {
                conv.put("lastMessage", "");
                conv.put("lastMessageTime", null);
                conv.put("lastMessageId", 0L);
                conv.put("lastFromUserId", null);
            }

            // 获取未读消息数
            LambdaQueryWrapper<ChatMessage> unreadWrapper = new LambdaQueryWrapper<>();
            unreadWrapper.eq(ChatMessage::getFromUserId, friend.getFriendId())
                    .eq(ChatMessage::getToUserId, userId)
                    .eq(ChatMessage::getIsRead, 0);
            int unreadCount = Math.toIntExact(baseMapper.selectCount(unreadWrapper));
            conv.put("unreadCount", unreadCount);

            conversations.add(conv);
        }

        // 按最后消息时间排序
        conversations.sort((a, b) -> {
            LocalDateTime timeA = (LocalDateTime) a.get("lastMessageTime");
            LocalDateTime timeB = (LocalDateTime) b.get("lastMessageTime");
            if (timeA == null && timeB == null) return 0;
            if (timeA == null) return 1;
            if (timeB == null) return -1;
            return timeB.compareTo(timeA);
        });

        return conversations;
    }

    @Override
    public int getUnreadCount(Long userId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getToUserId, userId).eq(ChatMessage::getIsRead, 0);
        return Math.toIntExact(baseMapper.selectCount(wrapper));
    }

    @Override
    public int getUnreadCountWithFriend(Long userId, Long friendId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getFromUserId, friendId)
                .eq(ChatMessage::getToUserId, userId)
                .eq(ChatMessage::getIsRead, 0);
        return Math.toIntExact(baseMapper.selectCount(wrapper));
    }

    @Override
    public Map<String, Object> markAsRead(Long userId, Long friendId) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getFromUserId, friendId)
                .eq(ChatMessage::getToUserId, userId)
                .eq(ChatMessage::getIsRead, 0);

        List<ChatMessage> messages = baseMapper.selectList(wrapper);
        for (ChatMessage msg : messages) {
            msg.setIsRead(1);
            msg.setReadTime(LocalDateTime.now());
            baseMapper.updateById(msg);
        }

        result.put("success", true);
        result.put("msg", "已标记为已读");
        result.put("count", messages.size());
        return result;
    }

    @Override
    public List<ChatMessage> getNewMessages(Long userId, Long lastMessageId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getToUserId, userId)
                .gt(ChatMessage::getId, lastMessageId)
                .orderByAsc(ChatMessage::getId);
        return baseMapper.selectList(wrapper);
    }
}
