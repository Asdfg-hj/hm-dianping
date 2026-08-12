package com.hmdp.utils;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;


/**
 * 登录拦截的拦截器
 * LoginInterceptor
 */
public class LoginInterceptor implements HandlerInterceptor {
    

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断是否要做拦截(ThreadLocal中是否有用户)
        if(UserHolder.getUser() == null){
            //为空，需要拦截，设置状态码
            response.setStatus(401);
            return false;
        }
        //有用户则放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //移除ThreadLocal中的用户信息，防止内存泄漏
        UserHolder.removeUser();
    }


}
