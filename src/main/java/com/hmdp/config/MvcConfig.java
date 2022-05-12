package com.hmdp.config;

import com.hmdp.utils.LoginInterceptor;
import com.hmdp.utils.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * @author kitto
 * @create 2022-04-27-9:37
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    //添加拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //第一个拦截器用于登录拦截
       registry.addInterceptor(new LoginInterceptor())
               //除了这些都拦截
               .excludePathPatterns(
                       "/shop/**",
                       "/voucher/**",
                       "/shop-type/**",
                       "/upload/**",
                       "/blog/hot",
                       "/user/code",
                       "/user/login"
               ).order(1);
       //用于token刷新拦截
       registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate)).order(0);//第二个拦截器
    }
}
