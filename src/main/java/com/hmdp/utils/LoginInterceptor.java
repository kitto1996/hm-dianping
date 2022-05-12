package com.hmdp.utils;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author kitto
 * 拦截器
 * @create 2022-04-27-9:25
 */
public class LoginInterceptor implements HandlerInterceptor {

 //private StringRedisTemplate stringRedisTemplate;

//    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
//        this.stringRedisTemplate = stringRedisTemplate;
//    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
/*
* 只有一个登录拦截器
* */

        //1获取请求头当中的token
//        String token = request.getHeader("authorization");
//           //2判断token是否为空
//        if (StrUtil.isBlank(token)) {
//            //3不存在就拦截,返回401状态码
//            response.setStatus(401);
//            return false;
//        }
//        //4基于token获取redis当中的用户
//        String key=RedisConstants.LOGIN_USER_KEY+token;
//        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
//
//        if(userMap.isEmpty()){
//        response.setStatus(401);
//        return false;
//    }
//        //5将查询到的数据转换成UserDTO
//        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
//        //6存储用户信息到ThreadLocal当中
//        UserHolder.saveUser((userDTO));
//        //7刷新token有效期
//        stringRedisTemplate.expire(key,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        //8放行
        /*
        * 有两个拦截器
        * */
        if (UserHolder.getUser()==null){
            //如果user==null，说明没有设置状态码
          response.setStatus(401);
          //拦截
          return false;
        }
        //放行
        return  true;
    }

//    @Override
//    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
//       //移除用户
//        UserHolder.removeUser();
//    }
}
