package com.hmdp.utils;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;


import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.hmdp.dto.UserDTO;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 登录拦截的拦截器
 * LoginInterceptor
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {
    //不能进行依赖注入  
    private StringRedisTemplate stringRedisTemplate;
    
    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //从请求头中获取token
        String token = request.getHeader("authorization");
        //判断token是否为空
        if(StrUtil.isBlank(token)){
            //放行，让后面的拦截器进行判断
            return true;
        }
        String key = RedisConstants.LOGIN_USER_KEY + token;
        //获取reids中的用户
        Map<Object,Object> userMap = stringRedisTemplate.opsForHash()
                .entries(key);
        //判断用户是否存在
        if(userMap.isEmpty()){
            return true;
        }
        //将查询到的hash数据转为UserDTO对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        //存在，保存用户信息到ThreadLocal
        UserHolder.saveUser(userDTO);
        //刷新token有效期 ，30分钟无任何操作则过期
        stringRedisTemplate.expire(key, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        //放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //移除ThreadLocal中的用户信息，防止内存泄漏
        UserHolder.removeUser();
    }


}
