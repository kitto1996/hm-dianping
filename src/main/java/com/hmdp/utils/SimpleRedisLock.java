package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author kitto
 * @create 2022-05-11-9:46
 */
public class SimpleRedisLock implements ILock{
    private String name;
    private StringRedisTemplate stringRedisTemplate;
    private static final String KEY_PREFIX="lock:";
    //线程标识ID
    private static final String ID_PREFIX= UUID.randomUUID().toString(true)+"-";
    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        //1.获取线程标识
       String id = ID_PREFIX+Thread.currentThread().getId();
        //获取锁
        Boolean success = stringRedisTemplate.opsForValue().
                setIfAbsent(KEY_PREFIX + name, id, timeoutSec,
                        TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        //获取线程标识
        String id =ID_PREFIX+Thread.currentThread().getId();
        //判断标识是否一致
        String lockId = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
        if(id.equals(lockId)) {
            //释放锁
            stringRedisTemplate.delete(KEY_PREFIX + name);
        }
    }
}
