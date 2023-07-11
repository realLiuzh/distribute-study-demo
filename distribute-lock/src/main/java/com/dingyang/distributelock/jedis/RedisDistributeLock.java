package com.dingyang.distributelock.jedis;

import com.dingyang.distributelock.inter.DistributeLock;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class RedisDistributeLock implements DistributeLock {

    public static final int RETRY_INTERVAL = 1;

    public static final int LOCK_EXPIRE_TIME = 3000;


    public static final Map<Thread, LockData> THREAD_LOCK_DATA = new ConcurrentHashMap<>();

    public static final String LUA_LOCK_SCRIPT = "local r=tonumber(redis.call('SETNX',KEYS[1],ARGV[1]))\n" + "redis.call('EXPIRE',KEYS[1],ARGV[2])\n" + "return r";

    public static final String LUA_UNLOCK_SCRIPT = "if redis.call('GET',KEYS[1]) == ARGV[1] then\n" +
            "return redis.call('DEL',KEYS[1])\n" +
            "end\n" +
            "return 0";

    private final JedisPool jedisPool;

    public RedisDistributeLock(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public boolean lock(String lockKey, int time, TimeUnit timeUnit) {
        LockData lockData = THREAD_LOCK_DATA.get(Thread.currentThread());
        if (lockData != null) {
            lockData.getLockCount().incrementAndGet();
            return true;
        }
        String lockValue = tryLock(lockKey, time, timeUnit);
        if (lockValue != null) {
            LockData myLockData = new LockData(Thread.currentThread(), lockValue);
            THREAD_LOCK_DATA.put(Thread.currentThread(), myLockData);
            return true;
        }
        return false;
    }


    public String tryLock(String lockKey, int time, TimeUnit timeUnit) {

        try (Jedis jedis = jedisPool.getResource()) {
            String lockValue = System.currentTimeMillis() + "-" + new Random().nextInt(100000);
            long result = 0, startTime = System.currentTimeMillis();
            List<String> luaKeys = new ArrayList<>();
            luaKeys.add(lockKey);
            List<String> luaArgs = new ArrayList<>();
            luaArgs.add(lockValue);
            luaArgs.add(String.valueOf(LOCK_EXPIRE_TIME));
            // 加锁失败后的重试策略
            while (result == 0) {
                result = (long) jedis.eval(LUA_LOCK_SCRIPT, luaKeys, luaArgs);
                if (result == 1) {
                    return lockValue;
                }
                if (System.currentTimeMillis() - startTime > timeUnit.toMillis(time) - TimeUnit.SECONDS.toMillis(RETRY_INTERVAL)) {
                    break;
                }
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(RETRY_INTERVAL));
            }
        }
        return null;
    }

    public void unlock(String lockId) {
        LockData lockData = THREAD_LOCK_DATA.get(Thread.currentThread());
        if (lockData == null) {
            throw new IllegalMonitorStateException("you don't have the lock: " + lockId);
        }
        if (lockData.getLockCount().get() <= 0) {
            throw new IllegalMonitorStateException("lock frequency has some error. lock id: " + lockId);
        }
        if (lockData.getLockCount().get() > 0) {
            lockData.getLockCount().decrementAndGet();
        }
        if (lockData.getLockCount().get() == 0) {
            try (Jedis jedis = jedisPool.getResource()) {
                ArrayList<String> luaKeys = new ArrayList<>();
                luaKeys.add(lockId);
                ArrayList<String> luaArgs = new ArrayList<>();
                luaArgs.add(lockData.getLockVal());
                jedis.eval(LUA_UNLOCK_SCRIPT, luaKeys, luaArgs);
                THREAD_LOCK_DATA.remove(Thread.currentThread());
            }
        }
    }


}
