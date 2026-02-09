package com.example.springjwt.api;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ApiUsageLimiter {
    private final Map<LocalDate, AtomicInteger> usageMap = new ConcurrentHashMap<>();
    private final int DAILY_LIMIT = 100; // 일일 제한

    public boolean canUse() {
        LocalDate today = LocalDate.now();
        usageMap.putIfAbsent(today, new AtomicInteger(0));
        return usageMap.get(today).get() < DAILY_LIMIT;
    }

    public void increment() {
        LocalDate today = LocalDate.now();
        usageMap.get(today).incrementAndGet();
    }
}
