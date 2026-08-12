package com.hmdp.service;


import javax.servlet.http.HttpSession;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

    /**
     * 发送手机验证码
     * @param phone 手机号
     * @param session 会话
     */
    Result sendCode(String phone,HttpSession session);

    /**
     * 用户登录
     * @param loginForm 登录参数
     * @param session 会话
     * @return 结果
     */
    Result login(LoginFormDTO loginForm, HttpSession session);
    
}
