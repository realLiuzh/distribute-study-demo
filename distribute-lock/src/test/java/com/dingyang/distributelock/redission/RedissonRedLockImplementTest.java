package com.dingyang.distributelock.redission;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.TimeUnit;

@Slf4j
public class RedissonRedLockImplementTest {
    @Test
    public void test_redisson_redLock() {
        Config config1 = new Config();
        config1.useSingleServer().setAddress("redis://47.96.86.132:6379");
        RedissonClient redissonClient1 = Redisson.create(config1);


        String lockKey = "redisson-distribute-lock";
        RLock lock1 = redissonClient1.getLock(lockKey);


        RedissonRedLock redLock = new RedissonRedLock(lock1);

        try {
            if (redLock.tryLock(10, 5, TimeUnit.SECONDS)) {
                log.info("lock success");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }
}
