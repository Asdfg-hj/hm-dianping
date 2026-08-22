package com.hmdp.utils;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;

public class SimpleRedisLock implements ILock {

    private String name;
    private StringRedisTemplate stringRedisTemplate;
    private static final String KEY_PREFIX = "lock";

    @Override
    public boolean tryLock(long timeoutSec){
        //获取线程标识
        Long threadId = Thread.currentThread().getId();
        //获取锁
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name,threadId + "",timeoutSec,TimeUnit.SECONDS);
        //预防空指针
        return Boolean.TRUE.equals(success);
    }
    
    @Override
    public void unlock(){
        stringRedisTemplate.delete(KEY_PREFIX);

    }



}
