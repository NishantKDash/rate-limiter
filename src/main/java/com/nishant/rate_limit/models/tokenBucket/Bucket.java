package com.nishant.rate_limit.models.tokenBucket;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public class Bucket {
	private final int capacity;
	private final int refillTokens;
	private final long refillIntervalInMillis;
	private AtomicInteger tokens;
	private long lastRefilltime;

	public Bucket(int capacity, int refillTokens, long refillIntervalInMillis) {
		this.capacity = capacity;
		this.refillTokens = refillTokens;
		this.refillIntervalInMillis = refillIntervalInMillis;
		this.tokens = new AtomicInteger(capacity);
		this.lastRefilltime = Instant.now().toEpochMilli();

	}

	public synchronized boolean acquireToken() {
		refill();
		if (tokens.get() > 0) {
			tokens.decrementAndGet();
			return true;
		}
		return false;
	}

	private void refill() {
		long now = Instant.now().toEpochMilli();
		long durationSinceLastRefill = now - this.lastRefilltime;
		if (durationSinceLastRefill > this.refillIntervalInMillis) {
			int refillMultiplier = (int) (durationSinceLastRefill / refillIntervalInMillis);
			int newTokens = Math.min(tokens.get() + refillTokens * refillMultiplier, capacity);
			tokens.set(newTokens);
			this.lastRefilltime = now;
		}
	}
}
