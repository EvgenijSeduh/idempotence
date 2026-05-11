package com.idempotence.idempotencelib.spring.web;

import com.idempotence.idempotencelib.annotation.Idempotent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/orders")
public class TestOrderController {

    private final AtomicInteger counter = new AtomicInteger();
    private volatile CountDownLatch slowRequestStarted = new CountDownLatch(0);
    private volatile CountDownLatch slowRequestRelease = new CountDownLatch(0);

    @PostMapping
    @Idempotent
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> request) {
        int current = counter.incrementAndGet();
        return ResponseEntity.status(201).body(Map.of("orderId", 42, "callCount", current, "amount", request.get("amount")));
    }

    @PostMapping("/slow")
    @Idempotent
    public ResponseEntity<Map<String, Object>> createSlowOrder(@RequestBody Map<String, Object> request)
            throws InterruptedException {
        int current = counter.incrementAndGet();
        slowRequestStarted.countDown();
        slowRequestRelease.await(5, TimeUnit.SECONDS);
        return ResponseEntity.status(201).body(Map.of("orderId", 99, "callCount", current, "amount", request.get("amount")));
    }

    public int getCounter() { return counter.get(); }
    public void resetCounter() {
        counter.set(0);
        slowRequestStarted = new CountDownLatch(0);
        slowRequestRelease = new CountDownLatch(0);
    }
    public void prepareSlowRequestBlocking() {
        slowRequestStarted = new CountDownLatch(1);
        slowRequestRelease = new CountDownLatch(1);
    }
    public boolean awaitSlowRequestStarted(long timeoutMillis) throws InterruptedException {
        return slowRequestStarted.await(timeoutMillis, TimeUnit.MILLISECONDS);
    }
    public void releaseSlowRequest() { slowRequestRelease.countDown(); }
}
