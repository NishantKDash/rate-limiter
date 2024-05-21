package com.nishant.rate_limit.services;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.nishant.rate_limit.models.leakingBucket.Bucket;
import com.nishant.rate_limit.models.leakingBucket.Queue;
import com.nishant.rate_limit.models.leakingBucket.Request;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

public class LeakingBucketRateLimitingService {

	private final Map<String, Bucket> buckets;
	private final Queue queue;
	private final int capacity;
	private final int refillTokens;
	private final long refillIntervalInMillis;
	private final ScheduledExecutorService executorService;

	public LeakingBucketRateLimitingService(int capacity, int refillTokens, long refillIntervalInMillis,
			long processRequestInterval) {
		this.buckets = new ConcurrentHashMap<String, Bucket>();
		this.queue = new Queue();
		this.capacity = capacity;
		this.refillIntervalInMillis = refillIntervalInMillis;
		this.refillTokens = refillTokens;
		executorService = Executors.newScheduledThreadPool(1);
		executorService.scheduleAtFixedRate(() -> {
			try {
				processRequests();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ServletException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}, 0, processRequestInterval, TimeUnit.MILLISECONDS);
	}

	public boolean consume(String key) {
		Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity, refillTokens, refillIntervalInMillis));
		bucket = buckets.get(key);
		return bucket.acquireToken();
	}

	public int getCapacity() {
		return this.capacity;
	}

	public long getInterval() {
		return this.refillIntervalInMillis;
	}

	public boolean consume(String key, ServletRequest request, ServletResponse response, FilterChain chain) {
		if (consume(key)) {
			this.queue.insert(request, response, chain);
			return true;
		}
		return false;
	}

	public void processRequests() throws IOException, ServletException {
		while (this.queue.getSize() > 0) {
			Request request = this.queue.process();
			request.getFilterChain().doFilter(request.getServletRequest(), request.getServletResponse());
		}
	}

	public void shutdown() {
		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
				executorService.shutdownNow();
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow();
		}
	}

}
