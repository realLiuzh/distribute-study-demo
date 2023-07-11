package com.dingyang.distributelock.inter;

import java.util.concurrent.TimeUnit;

public interface DistributeLock {

    boolean lock(String lockKey, int time, TimeUnit timeUnit);

    void unlock(String lockKey);
}
