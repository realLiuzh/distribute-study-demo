package com.dingyang.distributelock.jedis;

import com.dingyang.distributelock.inter.Callback;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

public class RedisDistributeLockTemplateTest {
    @Test
    public void test_redis_lua_lock() {
        JedisPool jedisPool = new JedisPool("47.96.86.132", 6379);
        Jedis jedis = jedisPool.getResource();
        String lockKey = "dingyang-test-lock-key8", lockValue = "dingyang-test-lock-value";
        //  EXPIRE是设置秒级的过期时间
        //  PEXPIRE是设置毫秒级别的过期时间
        String luaLockScript = "local r=tonumber(redis.call('SETNX',KEYS[1],ARGV[1]));" +
                "redis.call('EXPIRE',KEYS[1],ARGV[2]);" +
                "return r;";
        ArrayList<String> luaKeys = new ArrayList<>();
        luaKeys.add(lockKey);
        ArrayList<String> luaArgs = new ArrayList<>();
        luaArgs.add(lockValue);
        luaArgs.add(String.valueOf(3000));
        Long redisOperationResult = (Long) jedis.eval(luaLockScript, luaKeys, luaArgs);
        jedis.close();
        assert redisOperationResult == 1;
    }


    @Test
    public void test_redis_lua_unlock() {
        JedisPool jedisPool = new JedisPool("47.96.86.132", 6379);
        Jedis jedis = jedisPool.getResource();
        String luaUnlockScript = "local v=redis.call('GET',KEYS[1])\n" +
                "local r= 0\n" +
                "if v == ARGV[1] then\n" +
                "r = redis.call('DEL',KEYS[1])\n" +
                "end\n" +
                "return r";
        ArrayList<String> luaKeys = new ArrayList<>();
        luaKeys.add("dingyang-test-lock-key5");
        ArrayList<String> luaArgv = new ArrayList<>();
        luaArgv.add("dingyang-test-lock-value");
        long redisOpResult = (long) jedis.eval(luaUnlockScript, luaKeys, luaArgv);
        jedis.close();
        assert redisOpResult == 1;
    }

    @Test
    public void test_redis_command_lock() {
        JedisPool jedisPool = new JedisPool("47.96.86.132", 6379);
        Jedis jedis = jedisPool.getResource();
        String lockKey = "dingyang-test-lock-key", lockValue = "dingyang-test-lock-value";
        Long redisOpResult = jedis.setnx(lockKey, lockValue);
        jedis.close();
        assert redisOpResult == 1;

    }


    @Test
    public void test_my_redis_distribute_lock_template() throws InterruptedException {
        int size = 3;
        JedisPool jedisPool = new JedisPool("47.96.86.132", 6379);
        CountDownLatch countDownLatch = new CountDownLatch(size);
        ArrayList<Future<Object>> futureList = new ArrayList<>();
        ExecutorService threadPool = Executors.newCachedThreadPool();
        RedisDistributeLockTemplate lockTemplate = new RedisDistributeLockTemplate(jedisPool);
        Callable<Object> callable = () -> lockTemplate.execute("dingyang-test-4444", 4, TimeUnit.SECONDS, new Callback() {
            @Override
            public Object onGetLock() {
                System.out.println(Thread.currentThread().getName() + "get lock success");
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                countDownLatch.countDown();
                return Thread.currentThread().getName();
            }

            @Override
            public void onTimeout() {
                System.out.println(Thread.currentThread().getName() + "get lock fail");
                countDownLatch.countDown();
            }
        });

        for (int i = 0; i < size; i++) {
            futureList.add(threadPool.submit(callable));
        }

        countDownLatch.await();
        for (Future<Object> future : futureList) {
            try {
                System.out.println(future.get());
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }


    @Test
    public void test_timeunit() {
        long millis = TimeUnit.SECONDS.toMillis(1);
        assert millis == 1000;
    }


    @Test
    public void test_jedis_name(){
        JedisPool jedisPool = new JedisPool("47.96.86.132", 6379);
        Jedis jedis = jedisPool.getResource();
        jedis.clientSetname("jedis-1");
        jedis.close();
        System.out.println(jedis.clientGetname());
    }
}
