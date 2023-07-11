package com.dingyang.distributelock.redission;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;


@Slf4j
public class RedissonDistributeLockTemplateTest {


    private RedissonClient redisson;

    @Before
    public void initRedissonClient() {
        // create config object
        Config redissonConfig = new Config();
        // single Server
        redissonConfig.useSingleServer().setAddress("redis://47.96.86.132:6379");

        // create redisson instance
        redisson = Redisson.create(redissonConfig);
    }


    @Test
    public void test_redisson_reentrant_lock() throws InterruptedException {
        int size = 4;
        String lockKey = "redisson-" + System.currentTimeMillis() + "-" + new Random().nextInt(10000);
        RLock redissonLock = redisson.getLock(lockKey);
        ExecutorService threadPool = Executors.newCachedThreadPool();
        CountDownLatch countDownLatch = new CountDownLatch(size);
        for (int i = 0; i < size; i++) {
            threadPool.submit(() -> {
                redissonLock.lock();
                try {
                    log.info("{} get lock", Thread.currentThread().getName());
                    LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));
                } finally {
                    redissonLock.unlock();
                    log.info("{} unlock", Thread.currentThread().getName());
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
    }
}
