package com.dingyang.distributelock.inter;

/**
 * Created by sunyujia@aliyun.com on 2016/2/23.
 */
public interface Callback {

    Object onGetLock() throws InterruptedException;

    void onTimeout() throws InterruptedException;
}
