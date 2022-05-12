package com.hmdp.utils;

/**
 * @author kitto
 * @create 2022-05-11-9:43
 */
public interface ILock {
    /*
    * 获取锁
    * @param timeoutSec获取锁的超时时间，过期自动释放
    * @return true获取成功 false 表示失败
    * */
    boolean tryLock(long timeoutSec);
    /*
    * 释放锁
    * */
    void unlock();
}
