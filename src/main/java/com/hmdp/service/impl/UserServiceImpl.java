package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;


/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            //2不符合，提示信息
            return Result.fail("手机号格式不正确");
        }
        //3复合，生成验证码
        String code = RandomUtil.randomNumbers(6);
        //保存验证码到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //4保存验证码到session
//        session.setAttribute("code", code);
        //5发送验证码，到控制台
        log.debug("发送验证码成功,验证码:{}", code);
        //返回
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        //1校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            //2不符合，提示信息
            return Result.fail("手机号格式不正确");
        }
        //2从redis获取校验码并进行校验
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();

        if (cacheCode == null || !cacheCode.equals(code)) {
            //3两者不一样，报错
            return Result.fail("验证码错误");
        }
        //4一致，查询用户 select * from user where phone=?
        User user = query().eq("phone", phone).one();
        //5查询用户是否存在
        if (user == null) {
            //6用户不存在，把用户存入数据库
            user = createUserWithPhone(phone);
        }
        //7存在，就把用户信息存入redis

        //7.1随机生成token,作为登录令牌
        String token = UUID.randomUUID().toString(true);
        //7.2将user对象转换成hash存储
        //BeanUtils.copyProperties("转换前的类", "转换后的类");
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //将user对象转换成map
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().
                setIgnoreNullValue(true).setFieldValueEditor((fieldName,filedValue)->filedValue.toString()));
        //存储
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, userMap);
        //设置token有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);
        //返回token
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        //创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName("user" + RandomUtil.randomString(5));
        //保存用户
        save(user);
        //返回
        return user;
    }
}
