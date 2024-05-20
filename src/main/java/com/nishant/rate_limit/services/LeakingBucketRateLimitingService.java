package com.nishant.rate_limit.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.nishant.rate_limit.models.leakingBucket.Bucket;
import com.nishant.rate_limit.models.leakingBucket.Queue;
import com.nishant.rate_limit.models.leakingBucket.Request;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

public class LeakingBucketRateLimitingService {

	private final Map<String, Bucket> buckets;
	private final Queue queue;
	private final int capacity;
	private final int refillTokens;
	private final long refillIntervalInMillis;

	public LeakingBucketRateLimitingService(int capacity, int refillTokens, long refillIntervalInMillis) {
		this.buckets = new ConcurrentHashMap<String, Bucket>();
		this.queue = new Queue();
		this.capacity = capacity;
		this.refillIntervalInMillis = refillIntervalInMillis;
		this.refillTokens = refillTokens;
	}

	public boolean consume(String key) {
		if (!buckets.containsKey(key)) {
			buckets.put(key, new Bucket(capacity, refillTokens, refillIntervalInMillis));
		}
		Bucket bucket = buckets.get(key);
		return bucket.acquireToken();
	}

	public int getCapacity() {
		return this.capacity;
	}

	public long getInterval() {
		return this.refillIntervalInMillis;
	}

	public int getCurrentSize() {
		return this.queue.getSize();
	}

	public boolean consume(String key, ServletRequest request, ServletResponse response) {
		if (consume(key)) {
			this.queue.insert(request, response);
			return true;
		}
		return false;
	}

	public Request processRequests() {
		return this.queue.process();
		}

}
