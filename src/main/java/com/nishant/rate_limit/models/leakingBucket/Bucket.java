package com.nishant.rate_limit.models.leakingBucket;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public class Bucket {
	private final int capacity;
	private final int refillTokens;
	private final long refillIntervalInMillis;
	private  long lastRefillTime;
	private AtomicInteger tokens;

	public Bucket(int capacity, int refillTokens, long refillIntervalInMillis) {
		this.capacity = capacity;
		this.refillTokens = refillTokens;
		this.refillIntervalInMillis = refillIntervalInMillis;
		this.lastRefillTime = Instant.now().toEpochMilli();
		this.tokens = new AtomicInteger(capacity);
	}

	public synchronized boolean acquireToken() {
		refill();
		if (tokens.get() > 0) {
			tokens.decrementAndGet();
			return true;
		}
		return false;
	}

	public void refill() {
		long now = Instant.now().toEpochMilli();
		long durationSinceLastRefill = now - this.lastRefillTime;
		if (durationSinceLastRefill > this.refillIntervalInMillis) {
			int refillMultiplier = (int) (durationSinceLastRefill / refillIntervalInMillis);
			int newTokens = Math.min(tokens.get() + refillTokens * refillMultiplier, capacity);
			tokens.set(newTokens);
			this.lastRefillTime = now;
		}
	}

}
