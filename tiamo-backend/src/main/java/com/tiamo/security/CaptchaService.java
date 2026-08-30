package com.tiamo.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码服务（内存存储，生产环境建议用 Redis）
 * 存储手机号 -> 验证码，带过期时间
 */
@Component
public class CaptchaService {

    /** 验证码有效期：5分钟 */
    private static final long EXPIRE_TIME = 5 * 60 * 1000L;

    /** key: 手机号, value: [验证码, 生成时间戳] */
    private final Map<String, String[]> captchaStore = new ConcurrentHashMap<>();

    /**
     * 生成并存储验证码
     * @param phone 手机号
     * @return 生成的6位验证码
     */
    public String generateCaptcha(String phone) {
        String captcha = String.format("%06d", (int) (Math.random() * 1000000));
        captchaStore.put(phone, new String[]{captcha, String.valueOf(System.currentTimeMillis())});
        // 实际项目中这里调用短信服务商发送验证码
        // 为了演示，打印到控制台
        System.out.println("【验证码】手机号: " + phone + " 验证码: " + captcha);
        return captcha;
    }

    /**
     * 验证验证码
     * @param phone 手机号
     * @param captcha 用户输入的验证码
     * @return 是否验证通过
     */
    public boolean validateCaptcha(String phone, String captcha) {
        String[] stored = captchaStore.get(phone);
        if (stored == null) return false;

        String storedCaptcha = stored[0];
        long generateTime = Long.parseLong(stored[1]);

        // 检查是否过期
        if (System.currentTimeMillis() - generateTime > EXPIRE_TIME) {
            captchaStore.remove(phone);
            return false;
        }

        // 验证成功后删除（一次性使用）
        if (storedCaptcha.equals(captcha)) {
            captchaStore.remove(phone);
            return true;
        }
        return false;
    }
}
