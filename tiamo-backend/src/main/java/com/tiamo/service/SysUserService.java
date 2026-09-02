package com.tiamo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tiamo.entity.SysUser;

/**
 * 系统用户 Service 接口
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 根据用户名查询
     */
    SysUser getByUsername(String username);

    /**
     * 根据手机号查询
     */
    SysUser getByPhone(String phone);

    /**
     * 根据邮箱查询
     */
    SysUser getByEmail(String email);

    /**
     * 用户注册
     */
    boolean register(SysUser user);

    /**
     * 修改密码
     */
    boolean updatePassword(Long userId, String newPassword);

    /**
     * 通过手机号重置密码
     */
    boolean resetPasswordByPhone(String phone, String newPassword);

    /**
     * 通过邮箱重置密码
     */
    boolean resetPasswordByEmail(String email, String newPassword);
}
