package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @author kitto
 * @create 2022-05-08-10:40
 */
@Component
public class RedisIdWorker {
    //开始时间戳
    private static final long BEGIN_TIME = 1640995200L;
    //序列号位数
    private static final int BIT_LENGTH = 32;
    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String Prefix) {

        //1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long result = nowSecond - BEGIN_TIME;
        //2.生成序列号
        //2.1获取当前日期，精确到天
        String data = now.format(DateTimeFormatter.ofPattern("yy:MM:dd"));
        //2.2自增
        long count = stringRedisTemplate.opsForValue().increment("inc:" + Prefix +":"+ data);
        //3.返回

        return result<<BIT_LENGTH|count;
    }

    public static void main(String[] args) {
        LocalDateTime dateTime = LocalDateTime.of(2022, 1, 1, 0, 0, 0);
        long second = dateTime.toEpochSecond(ZoneOffset.UTC);
        System.out.println("second=" + second);
    }
}
