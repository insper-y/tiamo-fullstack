package com.tiamo.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 邀请码服务
 * 内存存储邀请码，3分钟有效，使用一次后失效
 */
@Service
public class InviteCodeService {

    private static final ConcurrentHashMap<String, Long> inviteCodeMap = new ConcurrentHashMap<>();

    /**
     * 生成邀请码
     * @return 6位数字邀请码
     */
    public String generateCode() {
        long now = System.currentTimeMillis();
        // 清理过期邀请码
        inviteCodeMap.entrySet().removeIf(e -> e.getValue() < now);
        // 生成6位随机数字邀请码（确保不重复）
        java.util.Random random = new java.util.Random();
        String code;
        do {
            code = String.format("%06d", random.nextInt(1000000));
        } while (inviteCodeMap.containsKey(code));
        // 存储到内存，3分钟过期
        inviteCodeMap.put(code, now + 3 * 60 * 1000);
        return code;
    }

    /**
     * 验证邀请码是否有效
     * @param code 邀请码
     * @return 是否有效
     */
    public boolean validateCode(String code) {
        if (code == null || code.isEmpty()) return false;
        Long expireTime = inviteCodeMap.get(code);
        if (expireTime == null) return false;
        if (expireTime < System.currentTimeMillis()) {
            inviteCodeMap.remove(code);
            return false;
        }
        // 验证成功后删除（一次性使用）
        inviteCodeMap.remove(code);
        return true;
    }
}
