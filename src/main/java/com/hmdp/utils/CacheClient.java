package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

/**
 * @author kitto
 * @create 2022-05-06-13:32
 */
@Component
@Slf4j
public class CacheClient {
    private StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    public void setWithLogical(String key, Object value, Long time, TimeUnit unit) {
        //设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        //写入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    //缓存穿透工具类
    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type,
                                          Function<ID, R> function, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        //1从redis查询商品缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //2判断是否存在
        if (StrUtil.isNotBlank(json)) {
            //3存在直接返回
            return JSONUtil.toBean(json, type);
        }
        //判断命中是否是空值
        if (json != null) {
            //如果为空，则返回错误信息
            return null;
        }
        //4不存在查询数据库
        R r = function.apply(id);
        //数据库当中不存在就返回错误
        if (r == null) {
            //将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //存在就返回到redis
        this.set(key, r, time, unit);
        return r;
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public <R, ID> R queryWithLogicalExpire(String keyPrefix, ID id,
                                            Class<R> type, Function<ID, R> function,
                                            Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        //1从redis查询商品缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //2判断是否存在
        if (StrUtil.isBlank(json)) {
            //3存在直接返回
            return null;
        }
        //4命中需要把json数据反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime localDateTime = redisData.getExpireTime();
        //5判断是否过期
        if (localDateTime.isAfter(LocalDateTime.now())) {
            //5.1未过期，直接返回店铺信息
            return r;
        }
        //5.2已过期，则进行缓存重建

        //6缓存重建
        //6.1获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        //6.2判断是否获取所成功
        if (isLock) {
            //6.3成功，开启独立线程，进行缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    //1.查询数据库
                    R r1 = function.apply(id);
                    //2.写入redis
                    setWithLogical(key, r1, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException();
                } finally {
                    //释放锁
                    unLock(lockKey);
                }
            });
        }

        //6.4返回过期店铺信息
        //4不存在查询数据库
        return r;
    }

    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }
}
