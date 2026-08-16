package com.hmdp;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;

@SpringBootTest
class HmDianPingApplicationTests {
    @Resource
    private CacheClient cacheClient;

    @Resource
    private ShopServiceImpl shopService;
    @Resource
    private RedisIdWorker redisIdWorker;

    //线程池
    private ExecutorService es = Executors.newFixedThreadPool(500);
    @Test
    void testIdWorker() throws InterruptedException{
        //CountDownLatch（倒计时锁）：主要作用是协调主线程和子线程的执行顺序。
        //这里传入参数 300，代表我们有 300 个异步任务（任务计数器设为 300）。
        // 主线程会等待这 300 个任务全部执行完毕后才继续往下走。
        CountDownLatch latch = new CountDownLatch(300);

        // 300个线程，每个线程生成100个id
        // Runnable是一个函数式接口，表示一个可以在单独线程中执行的任务
        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");// 生成订单业务的唯一 ID
                System.out.println("ID: " + id);
            }
            latch.countDown();// 该线程的任务完成后，计数器 -1
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            // 
            es.submit(task);// 向线程池提交 300 个异步任务
        }
        latch.await();// 主线程在这里卡住/阻塞，直到 CountDownLatch 减为 0
        long end = System.currentTimeMillis();
        System.out.println("耗时: " + (end - begin));
    }


    @Test
    void testSaveShop() throws InterruptedException{
        Shop shop = shopService.getById(1L);
        cacheClient.setWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY + 1L,shop,10L,TimeUnit.SECONDS);

    }




}
