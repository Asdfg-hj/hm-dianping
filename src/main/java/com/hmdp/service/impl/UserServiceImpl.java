package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;
import static com.hmdp.utils.RedisConstants.LOGIN_CODE_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_CODE_TTL;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 发送手机验证码
     * @param phone 手机号
     * @param session 会话
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1. 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            //2. 如果不符合，返回错误信息
            return Result.fail("手机号格式错误");
        }
        //3. 符合，生成验证码
        String code = RandomUtil.randomNumbers(6);
        //4. 保存验证码到redis  set key value ex 2 * 60
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY  + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //5. 发送验证码
        log.debug("发送验证码成功: {}", code);

        return Result.ok();
    }
    /**
     * 用户登录
     * @param loginForm 登录参数
     * @param session 会话
     * @return 结果
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1.校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            //如果不符合，返回错误信息
            return Result.fail("手机号格式错误");
        }
        //2.从redis中获取并校验验证码
        String chachecode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);

        //3.验证码不一致,则报错
        if (chachecode == null  || !chachecode.equals(loginForm.getCode())) {
            return Result.fail("验证码错误");
        }
        //4.一致,根据手机号在数据库中查询用户 select * from tb_user where phone = ?
        User user = query().eq("phone",phone).one();
        //5.判断用户是否存在
        if (user == null) {
            //6.不存在,则创建新用户并保存
            user = CreateUserWithPhone(phone);
        }
        //7.保存用户信息到redis中
        //7.1.生成随机token,作为登录令牌
        String token = UUID.randomUUID().toString(true);
        //7.2.将User对象转换为UserDTO对象再将UserDTO对象转为HashMap存储
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        Map<String,Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        //7.3.存储数据到redis并设置token的有效期
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, userMap);
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);
        //8.返回token给前端
        return Result.ok(token);
    }
    private User CreateUserWithPhone(String phone) {
        //创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX  + RandomUtil.randomString(10));
        //保存用户(mybatis_plus在ServiceImpl中提供了save方法)
        //insert into tb_user(phone,nick_name) values(?,?)
        save(user);
        return user;
    }
	
}
