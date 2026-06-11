package com.pcm.kms.stress;

import com.pcm.kms.starter.KmsClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 百万级加解密压力测试
 * <p>
 * 测试场景：
 * 1. 百万次加密性能测试
 * 2. 百万次解密性能测试
 * 3. 百万次加解密闭环测试
 * 4. 并发加解密测试（多线程）
 * <p>
 * 启动方式：
 * java -jar pcm-kms-stress-test.jar --stress.enabled=true --stress.alias=my-aes-key --stress.count=1000000
 */
@Slf4j
@Component
public class StressTestRunner implements CommandLineRunner {

    @Autowired
    private KmsClient kmsClient;

    @Override
    public void run(String... args) {
        // 检查是否启用压力测试
        boolean enabled = Boolean.parseBoolean(getArg(args, "stress.enabled", "false"));
        if (!enabled) {
            log.info("压力测试未启用。启动参数加 --stress.enabled=true 开启");
            return;
        }

        String alias = getArg(args, "stress.alias", "stress-aes");
        int count = Integer.parseInt(getArg(args, "stress.count", "1000000"));
        int threads = Integer.parseInt(getArg(args, "stress.threads", "10"));
        String algorithm = getArg(args, "stress.algorithm", "AES");

        log.info("========================================");
        log.info("  PCM-KMS 百万级压力测试");
        log.info("========================================");
        log.info("密钥别名: {}", alias);
        log.info("测试数量: {}", count);
        log.info("并发线程: {}", threads);
        log.info("算法: {}", algorithm);
        log.info("========================================");

        // 先验证连通性
        try {
            KmsClient.CryptoResult testResult = kmsClient.encrypt("connectivity-test", alias);
            log.info("连通性验证通过，服务端响应正常");
        } catch (Exception e) {
            log.error("连通性验证失败，请检查 KMS 服务端是否启动、密钥别名是否正确: {}", e.getMessage());
            return;
        }

        // 执行测试
        try {
            // 测试1: 百万次加密
            encryptStressTest(alias, count, threads);

            // 测试2: 百万次加解密闭环
            encryptDecryptStressTest(alias, count, threads);

            // 测试3: 摘要性能
            digestStressTest(count, threads);

        } catch (Exception e) {
            log.error("压力测试执行异常", e);
        }

        log.info("========================================");
        log.info("  压力测试全部完成");
        log.info("========================================");
    }

    /**
     * 百万次加密压力测试
     */
    private void encryptStressTest(String alias, int count, int threads) {
        log.info("\n--- 测试1: 百万次加密性能 ---");
        AtomicLong totalNanos = new AtomicLong(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(count);

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                threads, threads, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(count + 1000),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        long startNanos = System.nanoTime();

        for (int i = 0; i < count; i++) {
            final int idx = i;
            pool.execute(() -> {
                try {
                    String plainText = "stress-test-data-" + idx + "-" + UUID.randomUUID().toString().substring(0, 8);
                    long t0 = System.nanoTime();
                    kmsClient.encrypt(plainText, alias);
                    long cost = System.nanoTime() - t0;
                    totalNanos.addAndGet(cost);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    if (failCount.get() <= 5) {
                        log.warn("加密失败[{}]: {}", idx, e.getMessage());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long totalMs = (System.nanoTime() - startNanos) / 1_000_000;
        long avgNs = successCount.get() > 0 ? totalNanos.get() / successCount.get() : 0;
        double tps = totalMs > 0 ? successCount.get() * 1000.0 / totalMs : 0;

        log.info("加密测试结果:");
        log.info("  总请求数: {}", count);
        log.info("  成功数: {}", successCount.get());
        log.info("  失败数: {}", failCount.get());
        log.info("  总耗时: {} ms", totalMs);
        log.info("  平均耗时: {} μs", avgNs / 1000);
        log.info("  TPS: {:.2f}", tps);

        pool.shutdown();
    }

    /**
     * 百万次加解密闭环压力测试
     */
    private void encryptDecryptStressTest(String alias, int count, int threads) {
        log.info("\n--- 测试2: 百万次加解密闭环性能 ---");

        // 先批量加密，收集密文
        int batchSize = Math.min(count, 100000);
        log.info("预生成 {} 条密文...", batchSize);
        List<String> cipherTexts = new CopyOnWriteArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            try {
                String plainText = "batch-data-" + i;
                KmsClient.CryptoResult result = kmsClient.encrypt(plainText, alias);
                cipherTexts.add(result.getCipherText());
            } catch (Exception e) {
                // 跳过
            }
        }
        log.info("预生成完成，实际密文数: {}", cipherTexts.size());

        // 百万次解密
        AtomicLong totalNanos = new AtomicLong(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(count);
        Random random = new Random();

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                threads, threads, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(count + 1000),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        long startNanos = System.nanoTime();

        for (int i = 0; i < count; i++) {
            pool.execute(() -> {
                try {
                    String cipherText = cipherTexts.get(random.nextInt(cipherTexts.size()));
                    long t0 = System.nanoTime();
                    kmsClient.decrypt(cipherText, alias);
                    long cost = System.nanoTime() - t0;
                    totalNanos.addAndGet(cost);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    if (failCount.get() <= 5) {
                        log.warn("解密失败: {}", e.getMessage());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long totalMs = (System.nanoTime() - startNanos) / 1_000_000;
        long avgNs = successCount.get() > 0 ? totalNanos.get() / successCount.get() : 0;
        double tps = totalMs > 0 ? successCount.get() * 1000.0 / totalMs : 0;

        log.info("加解密闭环测试结果:");
        log.info("  总请求数: {}", count);
        log.info("  成功数: {}", successCount.get());
        log.info("  失败数: {}", failCount.get());
        log.info("  总耗时: {} ms", totalMs);
        log.info("  平均耗时: {} μs", avgNs / 1000);
        log.info("  TPS: {:.2f}", tps);

        pool.shutdown();
    }

    /**
     * 摘要性能测试
     */
    private void digestStressTest(int count, int threads) {
        log.info("\n--- 测试3: 百万次摘要性能 (SM3) ---");

        AtomicLong totalNanos = new AtomicLong(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(count);

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                threads, threads, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(count + 1000),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        long startNanos = System.nanoTime();

        for (int i = 0; i < count; i++) {
            final int idx = i;
            pool.execute(() -> {
                try {
                    String plainText = "digest-data-" + idx;
                    long t0 = System.nanoTime();
                    kmsClient.digest(plainText, "SM3");
                    long cost = System.nanoTime() - t0;
                    totalNanos.addAndGet(cost);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long totalMs = (System.nanoTime() - startNanos) / 1_000_000;
        long avgNs = successCount.get() > 0 ? totalNanos.get() / successCount.get() : 0;
        double tps = totalMs > 0 ? successCount.get() * 1000.0 / totalMs : 0;

        log.info("摘要测试结果:");
        log.info("  总请求数: {}", count);
        log.info("  成功数: {}", successCount.get());
        log.info("  失败数: {}", failCount.get());
        log.info("  总耗时: {} ms", totalMs);
        log.info("  平均耗时: {} μs", avgNs / 1000);
        log.info("  TPS: {:.2f}", tps);

        pool.shutdown();
    }

    private String getArg(String[] args, String key, String defaultValue) {
        for (String arg : args) {
            if (arg.startsWith("--" + key + "=")) {
                return arg.substring(key.length() + 3);
            }
        }
        return defaultValue;
    }
}
