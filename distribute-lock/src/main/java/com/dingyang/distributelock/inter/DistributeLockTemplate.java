package com.dingyang.distributelock.inter;

import java.util.concurrent.TimeUnit;

public interface DistributeLockTemplate {

    Object execute(String lockKey, int time, TimeUnit timeUnit, Callback callback);
}
