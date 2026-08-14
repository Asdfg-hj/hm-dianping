package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.hmdp.utils.RedisConstants;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
	@Override
	public Result queryTypeList() {
        // 1. 从redis中查询商铺类型缓存
        String key = RedisConstants.CACHE_SHOP_TYPE_KEY;
        String typeListJson = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断是否存在，如果存在，直接返回
        if(StrUtil.isNotBlank(typeListJson)){
            List<ShopType> typeList = JSONUtil.toList(typeListJson, ShopType.class);
            return Result.ok(typeList);
        }
        // 3. 如果不存在，根据id查询数据库
        List<ShopType> typeList = query().orderByAsc("sort").list();
        // 4. 不存在，返回错误
        if(typeList == null){
            return Result.fail("商铺类型不存在");
        }
        // 5. 存在，写入redis缓存
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(typeList), RedisConstants.CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);
        // 6. 返回
        return Result.ok(typeList);
		
	}

}
