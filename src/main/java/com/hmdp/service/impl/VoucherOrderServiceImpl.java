package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    /*实现优惠卷秒杀*/
    @Override

    public Result seckillVoucher(Long voucherId) {
        //1.查询优惠卷
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        //2.判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            //秒杀活动开始时间在现在之后，则秒杀尚未开始
            return Result.fail("秒杀尚未开始");
        }
        //3.判断秒杀是否结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            //秒杀结束时间在当前时间之前，则活动结束
            return Result.fail("秒杀已经结束");
        }
        //4.判断库存是否充足
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足");
        }
        return createVoucherOrder(voucherId);
    }
    @Resource
    private RedissonClient redissonClient;
    @Transactional
    public  Result createVoucherOrder(Long voucherId){

        //1人1单查询
        Long userId = UserHolder.getUser().getId();
        //创建锁对象
        RLock simpleRedisLock = redissonClient.getLock("lock:order" + userId);
        //获取锁
        boolean isLock = simpleRedisLock.tryLock();
        //判断获取锁是否成功
        //获取失败
        if(!isLock){
            return Result.fail("你，不能重复下单");
        }
        //1.查询订单
        try {
            int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
            //2.判断是否存在
            if (count > 0) {
                //证明该用户已经参加过活动
                return Result.fail("用户已经参加过活动");
            }
            //5.扣减库存
            boolean success = seckillVoucherService.update()
                    .setSql("stock=stock-1") //set stock=stock-1  //eq等于,gt>
                    .eq("voucher_id", voucherId).gt("stock", 0)//where id=? and stock>0
                    .update();
            if (!success) {
                return Result.fail("库存不足");
            }
            //6.count<=0,允许创建订单
            VoucherOrder order = new VoucherOrder();
            //6.1订单id
            long orderId = redisIdWorker.nextId("order");
            order.setId(orderId);
            //用户id
            order.setUserId(userId);
            //代金券id
            order.setVoucherId(voucherId);
            save(order);
            //7.返回
            return Result.ok(orderId);
        }
        finally {
            simpleRedisLock.unlock();
        }
    }
//    @Transactional
//    public  Result createVoucherOrder(Long voucherId){
//
//        //1人1单查询
//        Long userId = UserHolder.getUser().getId();
//       //创建锁对象
//        SimpleRedisLock simpleRedisLock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
//        //获取锁
//        boolean isLock = simpleRedisLock.tryLock(1200);
//        //判断获取锁是否成功
//        //获取失败
//        if(!isLock){
//            return Result.fail("你，不能重复下单");
//        }
//        //1.查询订单
//          try {
//              int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
//              //2.判断是否存在
//              if (count > 0) {
//                  //证明该用户已经参加过活动
//                  return Result.fail("用户已经参加过活动");
//              }
//              //5.扣减库存
//              boolean success = seckillVoucherService.update()
//                      .setSql("stock=stock-1") //set stock=stock-1  //eq等于,gt>
//                      .eq("voucher_id", voucherId).gt("stock", 0)//where id=? and stock>0
//                      .update();
//              if (!success) {
//                  return Result.fail("库存不足");
//              }
//              //6.count<=0,允许创建订单
//              VoucherOrder order = new VoucherOrder();
//              //6.1订单id
//              long orderId = redisIdWorker.nextId("order");
//              order.setId(orderId);
//              //用户id
//              order.setUserId(userId);
//              //代金券id
//              order.setVoucherId(voucherId);
//              save(order);
//              //7.返回
//              return Result.ok(orderId);
//          }
//          finally {
//              simpleRedisLock.unlock();
//          }
//        }
//    @Transactional
//   public  Result createVoucherOrder(Long voucherId){
//
//        //1人1单查询
//        Long userId = UserHolder.getUser().getId();
//
//        synchronized (userId.toString().intern()){
//            //1.查询订单
//            int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
//            //2.判断是否存在
//            if(count>0){
//                //证明该用户已经参加过活动
//                return Result.fail("用户已经参加过活动");
//            }
//            //5.扣减库存
//            boolean success = seckillVoucherService.update()
//                    .setSql("stock=stock-1") //set stock=stock-1  //eq等于,gt>
//                    .eq("voucher_id", voucherId).gt("stock",0)//where id=? and stock>0
//                    .update();
//            if (!success) {
//                return Result.fail("库存不足");
//            }
//            //6.count<=0,允许创建订单
//            VoucherOrder order = new VoucherOrder();
//            //6.1订单id
//            long orderId = redisIdWorker.nextId("order");
//            order.setId(orderId);
//            //用户id
//            order.setUserId(userId);
//            //代金券id
//            order.setVoucherId(voucherId);
//            save(order);
//            //7.返回
//            return Result.ok(orderId);
//        }
//    }
}
