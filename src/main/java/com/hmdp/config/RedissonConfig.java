package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author kitto
 * @create 2022-05-12-11:44
 */
@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redisClient(){
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.41.128:6379");
        return Redisson.create(config);
    }
}
