package com.hmdp.utils;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.annotation.Resource;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.hmdp.entity.Shop;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 封装工具类
 * CacheClient
 */
@Component
@Slf4j
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key,Object value,Long time,TimeUnit unit){
        //第二个必须是string类型的，需要强转
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(value),time,unit);
    }
    public void setWithLogicalExpire(String key,Object value,Long time,TimeUnit unit){
        //设置逻辑过期，用RedisData
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        //第二个必须是string类型的，需要强转
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }

    //封装缓存穿透
    public <R,ID> R queryWithPassThrough(String keyPrefix,ID id,Class<R> type,
        Function<ID,R> dbFallback,Long time,TimeUnit unit){
        // 1. 从redis中查询缓存
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断是否存在，如果存在，直接返回
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }
        //判断命中的是否是空值(null和空字符串空格换行)
        if(json != null){
            //说明是空格或者换行
            return null;
        }
        // 3. 如果不存在，即json为null,查询数据库
        R r = dbFallback.apply(id);
        // 4. 不存在，返回错误
        if (r == null) {
            //将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "",RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
            //返回错误信息
            return null;
        }
        // 5. 存在，写入redis缓存
        this.set(key, r, time, unit);
        // 6. 返回
        return r;
    }

     private boolean tryLock(String key){

        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10,TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }
    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
     //基于逻辑过期解决缓存击穿
    public <R,ID> R  queryWithLogicalExpire(String keyprefix,ID id,Class<R> type,
        Function<ID,R> dbFallback,Long time,TimeUnit unit){
        // 1. 从redis中查询商铺缓存
        String key = keyprefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断是否命中，如果未命中，返回空
        if (StrUtil.isBlank(json)) {
            return null;
        }
        // 3.如果命中，把jason反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        R r = JSONUtil.toBean(data, type);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 4.判断缓存是否过期
        if(expireTime.isAfter(LocalDateTime.now())){
            // 4.1未过期 直接返回店铺信息
            return r;
        }
        // 4.2已过期 需要缓存重建
        // 5.缓存重建
        // 5.1 获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 5.2 判断是否获取锁成功
        if(isLock){
            // 5.3 成功，开启独立线程实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                //重建缓存
                try {
                    //查询数据库
                    R r1 = dbFallback.apply(id);
                    //写入缓存
                    this.setWithLogicalExpire(key, lockKey, time, TimeUnit.MINUTES);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally{
                    //释放锁
                    unlock(lockKey);
                }
            });
        }
        // 5.4 失败，返回商铺信息（过期的）
        return r;
    }

}
