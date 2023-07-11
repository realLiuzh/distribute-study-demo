package com.dingyang.distributelock.jedis;


import com.dingyang.distributelock.inter.Callback;
import com.dingyang.distributelock.inter.DistributeLockTemplate;
import redis.clients.jedis.JedisPool;

import java.util.Objects;
import java.util.concurrent.TimeUnit;


/**
 * 基于Redis实现的分布式锁模版
 */
public class RedisDistributeLockTemplate implements DistributeLockTemplate {

    private final JedisPool jedisPool;

    public RedisDistributeLockTemplate(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    /**
     * 锁定分布式锁的入口方法
     *
     * @param lockId   分布式锁id
     * @param callback 回调方法
     * @return 是否加锁成功
     */
    public Object execute(String lockId, int time, TimeUnit timeUnit, Callback callback) {
        assert !Objects.equals(lockId, "");
        assert callback != null;
        boolean getLock = false;
        RedisDistributeLock lock = new RedisDistributeLock(jedisPool);
        try {
            if (lock.lock(lockId, time, timeUnit)) {
                getLock = true;
                return callback.onGetLock();
            } else {
                callback.onTimeout();
                return null;
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (getLock) {
                lock.unlock(lockId);
            }
        }
        return null;
    }
}
