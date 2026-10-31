import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * EJ 78/80/81/82/83：共享可变状态用并发集合、executor 优于裸线程、
 * atomic computeIfAbsent 免双检、线程安全档位写进文档、holder 懒初始化。
 * JDK21+ 升级项：Thread.ofVirtual() 替 Thread，注意 synchronized 内阻塞会 pin 载体。
 */
public final class ConcurrencyExample {

    // 线程安全档位：thread-safe——内部只持有 ConcurrentHashMap，
    // 公开操作不依赖外部同步；可变状态只有 cache 一个且已被并发托管。

    private static final class LazyHolder {
        static final ConcurrencyExample INSTANCE = new ConcurrencyExample();
    }

    /** 83：holder idiom——类加载锁保证只初始化一次，无锁无 volatile。 */
    public static ConcurrencyExample instance() {
        return LazyHolder.INSTANCE;
    }

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /** 78/81：computeIfAbsent 原子完成"查-算-放"，免去双检锁与竞态重复计算。 */
    public String get(String key, java.util.function.Function<String, String> loader) {
        return cache.computeIfAbsent(key, loader);
    }

    /** 80：executor 管理线程生命周期；任务带超时，失败可恢复。 */
    public String expensiveGet(String key) throws InterruptedException, ExecutionException, TimeoutException {
        var pool = Executors.newFixedThreadPool(4);
        try {
            var future = pool.submit(() -> "v-" + key);
            return future.get(5, TimeUnit.SECONDS);
        } finally {
            pool.shutdown();
        }
    }

    public static void main(String[] args) throws Exception {
        var ex = instance();
        System.out.println(ex.get("k", k -> "loaded-" + k));
        System.out.println(ex.expensiveGet("slow"));

        // 84：不依赖线程调度——CountDownLatch 表达"等到就绪"，不是 sleep 猜时间。
        var ready = new CountDownLatch(1);
        var worker = new Thread(() -> {
            ready.countDown();                 // JDK17；JDK21+ 可写 Thread.ofVirtual().start(...)
        });
        worker.start();
        ready.await();                         // 等到的是条件而非时间
        worker.join();
        System.out.println("worker signaled readiness");
    }
}
