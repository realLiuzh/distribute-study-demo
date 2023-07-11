package com.dingyang.distributelock.jedis;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

@Data
public class LockData {
    private Thread ownerThread;

    private String lockVal;

    private AtomicInteger lockCount = new AtomicInteger(1);

    public LockData(Thread ownerThread, String lockVal) {
        this.ownerThread = ownerThread;
        this.lockVal = lockVal;
    }
}
