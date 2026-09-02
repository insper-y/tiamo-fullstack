package com.tiamo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tiamo.entity.Friend;
import com.tiamo.entity.FriendRequest;
import com.tiamo.entity.SysUser;
import com.tiamo.mapper.FriendMapper;
import com.tiamo.mapper.FriendRequestMapper;
import com.tiamo.mapper.SysUserMapper;
import com.tiamo.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 好友服务实现
 */
@Service
public class FriendServiceImpl extends ServiceImpl<FriendMapper, Friend> implements FriendService {

    @Autowired
    private FriendRequestMapper friendRequestMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 应用启动时自动建表
     */
    @PostConstruct
    public void init() {
        try {
            friendRequestMapper.createTableIfNotExists();
            baseMapper.createTableIfNotExists();
            System.out.println("[好友系统] 数据库表初始化完成");
        } catch (Exception e) {
            System.out.println("[好友系统] 数据库表初始化失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Map<String, Object> sendFriendRequest(Long fromUserId, String fromUsername, String toUsername, String message) {
        Map<String, Object> result = new HashMap<>();

        // 查找目标用户
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getUsername, toUsername);
        SysUser toUser = sysUserMapper.selectOne(userWrapper);

        if (toUser == null) {
            result.put("success", false);
            result.put("msg", "用户不存在");
            return result;
        }

        if (toUser.getId().equals(fromUserId)) {
            result.put("success", false);
            result.put("msg", "不能添加自己为好友");
            return result;
        }

        // 检查是否已经是好友
        if (isFriend(fromUserId, toUser.getId())) {
            result.put("success", false);
            result.put("msg", "已经是好友了");
            return result;
        }

        // 检查是否已有待处理的请求
        LambdaQueryWrapper<FriendRequest> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(FriendRequest::getFromUserId, fromUserId)
                .eq(FriendRequest::getToUserId, toUser.getId())
                .eq(FriendRequest::getStatus, 0);
        if (friendRequestMapper.selectCount(pendingWrapper) > 0) {
            result.put("success", false);
            result.put("msg", "已发送过好友请求，请等待对方处理");
            return result;
        }

        // 创建好友请求
        FriendRequest request = new FriendRequest();
        request.setFromUserId(fromUserId);
        request.setFromUsername(fromUsername);
        request.setToUserId(toUser.getId());
        request.setToUsername(toUsername);
        request.setMessage(message != null ? message : "请求添加好友");
        request.setStatus(0);
        request.setCreateTime(LocalDateTime.now());
        friendRequestMapper.insert(request);

        result.put("success", true);
        result.put("msg", "好友请求已发送");
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> acceptFriendRequest(Long requestId, Long userId) {
        Map<String, Object> result = new HashMap<>();

        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null) {
            result.put("success", false);
            result.put("msg", "请求不存在");
            return result;
        }

        if (!request.getToUserId().equals(userId)) {
            result.put("success", false);
            result.put("msg", "无权处理此请求");
            return result;
        }

        if (request.getStatus() != 0) {
            result.put("success", false);
            result.put("msg", "请求已处理");
            return result;
        }

        // 更新请求状态
        request.setStatus(1);
        request.setHandleTime(LocalDateTime.now());
        friendRequestMapper.updateById(request);

        // 建立双向好友关系
        // A -> B
        Friend friend1 = new Friend();
        friend1.setUserId(request.getFromUserId());
        friend1.setFriendId(request.getToUserId());
        friend1.setFriendUsername(request.getToUsername());
        friend1.setFriendNickname(request.getToUsername());
        friend1.setCreateTime(LocalDateTime.now());
        baseMapper.insert(friend1);

        // B -> A
        Friend friend2 = new Friend();
        friend2.setUserId(request.getToUserId());
        friend2.setFriendId(request.getFromUserId());
        friend2.setFriendUsername(request.getFromUsername());
        friend2.setFriendNickname(request.getFromUsername());
        friend2.setCreateTime(LocalDateTime.now());
        baseMapper.insert(friend2);

        result.put("success", true);
        result.put("msg", "已接受好友请求");
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> rejectFriendRequest(Long requestId, Long userId) {
        Map<String, Object> result = new HashMap<>();

        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null) {
            result.put("success", false);
            result.put("msg", "请求不存在");
            return result;
        }

        if (!request.getToUserId().equals(userId)) {
            result.put("success", false);
            result.put("msg", "无权处理此请求");
            return result;
        }

        if (request.getStatus() != 0) {
            result.put("success", false);
            result.put("msg", "请求已处理");
            return result;
        }

        request.setStatus(2);
        request.setHandleTime(LocalDateTime.now());
        friendRequestMapper.updateById(request);

        result.put("success", true);
        result.put("msg", "已拒绝好友请求");
        return result;
    }

    @Override
    public List<Map<String, Object>> getFriendList(Long userId) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, userId).orderByDesc(Friend::getCreateTime);
        List<Friend> friends = baseMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Friend friend : friends) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", friend.getId());
            item.put("friendId", friend.getFriendId());
            item.put("friendUsername", friend.getFriendUsername());
            item.put("friendNickname", friend.getFriendNickname());
            item.put("remark", friend.getRemark());
            item.put("createTime", friend.getCreateTime());
            result.add(item);
        }
        return result;
    }

    @Override
    public List<FriendRequest> getReceivedRequests(Long userId) {
        LambdaQueryWrapper<FriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRequest::getToUserId, userId).orderByDesc(FriendRequest::getCreateTime);
        return friendRequestMapper.selectList(wrapper);
    }

    @Override
    public List<FriendRequest> getSentRequests(Long userId) {
        LambdaQueryWrapper<FriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRequest::getFromUserId, userId).orderByDesc(FriendRequest::getCreateTime);
        return friendRequestMapper.selectList(wrapper);
    }

    @Override
    public int getPendingRequestCount(Long userId) {
        LambdaQueryWrapper<FriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRequest::getToUserId, userId).eq(FriendRequest::getStatus, 0);
        return Math.toIntExact(friendRequestMapper.selectCount(wrapper));
    }

    @Override
    @Transactional
    public Map<String, Object> deleteFriend(Long userId, Long friendId) {
        Map<String, Object> result = new HashMap<>();

        // 删除双向好友关系
        LambdaQueryWrapper<Friend> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId);
        baseMapper.delete(wrapper1);

        LambdaQueryWrapper<Friend> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(Friend::getUserId, friendId).eq(Friend::getFriendId, userId);
        baseMapper.delete(wrapper2);

        result.put("success", true);
        result.put("msg", "已删除好友");
        return result;
    }

    @Override
    public boolean isFriend(Long userId, Long friendId) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId);
        return baseMapper.selectCount(wrapper) > 0;
    }
}
