package com.nishant.rate_limit.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.nishant.rate_limit.models.tokenBucket.Bucket;

public class TokenBucketRateLimitingService implements RateLimitingService {
	private final Map<String, Bucket> buckets;
	private final int capacity;
	private final int refillTokens;
	private final long refillIntervalInMillis;

	public TokenBucketRateLimitingService(int capacity, int refillTokens, long refillIntervalInMillis) {
		buckets = new ConcurrentHashMap<>();
		this.capacity = capacity;
		this.refillTokens = refillTokens;
		this.refillIntervalInMillis = refillIntervalInMillis;
	}

	public Bucket update(String key) {
		if (!this.buckets.containsKey(key))
			this.buckets.put(key, new Bucket(this.capacity, this.refillTokens, this.refillIntervalInMillis));

		return this.buckets.get(key);
	}

	public boolean consume(String key) {
		Bucket bucket = update(key);
		return bucket.acquireToken();
	}

	public int getCapacity() {
		return this.capacity;
	}

	public long getInterval() {
		return this.refillIntervalInMillis;
	}
}
