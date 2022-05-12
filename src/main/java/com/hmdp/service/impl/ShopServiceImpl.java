package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
@Resource
private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;
    @Override
    public Result queryById(Long id) {
       //缓存穿透
        //Shop shop = queryWithPassThrough(id);
        //互斥锁解决缓存穿透
        //Shop shop = queryWithMeutex(id);
    //    Shop shop = queryWithLogicalExpire(id); //查询的是添加到redis商铺信息
        //解决缓存穿透
    Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);
        //逻辑过期解决缓存击穿
  //Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY,id,Shop.class,this::getById,20L,TimeUnit.SECONDS);
        if(shop==null){
            return Result.fail("店铺不存在");
        }
            //返回shop
              return Result.ok(shop);
    }
    private static final ExecutorService CACHE_REBUILD_EXECUTOR= Executors.newFixedThreadPool(10);
    public  Shop queryWithLogicalExpire(Long id){
        String key=CACHE_SHOP_KEY+id;
        //1从redis查询商品缓存
        String shopJSON = stringRedisTemplate.opsForValue().get(key);
        //2判断是否存在
        if (StrUtil.isBlank(shopJSON)) {
            //3存在直接返回
            return null;
        }
        //4命中需要把json数据反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJSON, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        LocalDateTime localDateTime = redisData.getExpireTime();
        //5判断是否过期
        if(localDateTime.isAfter(LocalDateTime.now())){
            //5.1未过期，直接返回店铺信息
            return shop;
        }
        //5.2已过期，则进行缓存重建

        //6缓存重建
        //6.1获取互斥锁
        String lockKey=LOCK_SHOP_KEY+id;
        boolean isLock = tryLock(lockKey);
        //6.2判断是否获取所成功
        if (isLock){
            //6.3成功，开启独立线程，进行缓存重建
            CACHE_REBUILD_EXECUTOR.submit(()->{
               try {
                   //重建缓存
                   this.saveShopToRedis(id, 20L);
               }
               catch (Exception e){
                   throw new RuntimeException();
               }
               finally {
                   //释放锁
                   unLock(lockKey);
               }
            });
        }
        return shop;
    }


//    public  Shop queryWithMeutex(Long id) {
//
//        String key = CACHE_SHOP_KEY + id;
//        //1从redis查询商品缓存
//        String shopJSON = stringRedisTemplate.opsForValue().get(key);
//        //2判断是否存在
//        if (StrUtil.isNotBlank(shopJSON)) {
//            //3存在直接返回
//            return JSONUtil.toBean(shopJSON, Shop.class);
//
//        }
//        //判断命中是否是空值
//        if (shopJSON != null) {
//            //如果为空，则返回错误信息
//            return null;
//        }
//        //4实现缓存重建
//        //4.1获取互斥锁
//        String lockKey = "lock:shop" + id;
//        Shop shop = null;
//        try {
//            boolean isLock = tryLock(lockKey);
//            //4.2判断是否获取成功
//            if (!isLock) {
//                //4.3若失败，则休眠并且进行重试
//                Thread.sleep(15);
//                return queryWithMeutex(id);
//            }
//
//            //4成功，根据id查询数据库
//            shop = getById(id);
//            Thread.sleep(200);
//            //数据库当中不存在就返回错误
//            if (shop == null) {
//                //将空值写入redis
//                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//                return null;
//            }
//            //存在就返回到redis
//            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        //释放互斥锁
//        finally {
//            unLock(lockKey);
//        }
//        //返回
//        return shop;
//    }
//    public  Shop queryWithPassThrough(Long id){
//        String key=CACHE_SHOP_KEY+id;
//        //1从redis查询商品缓存
//        String shopJSON = stringRedisTemplate.opsForValue().get(key);
//        //2判断是否存在
//        if (StrUtil.isNotBlank(shopJSON)) {
//            //3存在直接返回
//            return JSONUtil.toBean(shopJSON, Shop.class);
//
//        }
//        //判断命中是否是空值
//        if(shopJSON!=null){
//            //如果为空，则返回错误信息
//            return null;
//        }
//        //4不存在查询数据库
//        Shop shop = getById(id);
//        //数据库当中不存在就返回错误
//        if(shop==null){
//            //将空值写入redis
//            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
//            return null;
//        }
//        //存在就返回到redis
//        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        return shop;
//    }
private boolean tryLock(String key){
     Boolean flag=stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10,TimeUnit.SECONDS);
     return BooleanUtil.isTrue(flag);
}
private void unLock(String key){
        stringRedisTemplate.delete(key);
}
public void saveShopToRedis(Long id,Long expireSeconds) throws Exception{
        //查询店铺数据
    Shop shop = getById(id);
    Thread.sleep(200);
    //封装逻辑过期时间
    RedisData redisData = new RedisData();
    redisData.setData(shop);
    redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
    //写入redis
    stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
}
    @Override
    public Result update(Shop shop) {
      Long id=shop.getId();
      if (id==null){
          return Result.fail("店铺id不能为空");
      }
      else {
          //1更新数据库
          updateById(shop);
          //2删除缓存
          stringRedisTemplate.delete(CACHE_SHOP_KEY+id);
          return Result.ok();
      }
    }
}
