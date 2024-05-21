package com.nishant.rate_limit.services;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.nishant.rate_limit.models.leakingBucket.Queue;
import com.nishant.rate_limit.models.leakingBucket.Request;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class DistributedLeakingBucketRateLimitingService {
	private final JedisPool jedisPool;
	private final int capacity;
	private final int refillTokens;
	private final long refillIntervalInMillis;
	private final Queue queue;
	private final ScheduledExecutorService executorService;

	public DistributedLeakingBucketRateLimitingService(String connectionUrl, int capacity, int refillTokens,
			long refillIntervalInMillis, long processRequestInterval) {
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(100);
		config.setMaxIdle(50);
		config.setMinIdle(10);
		config.setMaxWait(Duration.ofMillis(3000));
		config.setTestOnBorrow(true);
		config.setTestOnReturn(true);
		config.setTestWhileIdle(true);

		this.jedisPool = new JedisPool(config, connectionUrl);
		this.capacity = capacity;
		this.refillTokens = refillTokens;
		this.refillIntervalInMillis = refillIntervalInMillis;
		this.queue = new Queue();
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
	
	private void initializeBucket(String key) {
		try (Jedis jedis = jedisPool.getResource()) {
			String bucketKey = key;
			jedis.hset(bucketKey, "tokens", String.valueOf(capacity));
			jedis.hset(bucketKey, "lastRefillTime", String.valueOf(System.currentTimeMillis()));
		}
	}

	public boolean consume(String key) {
		String bucketKey = key;
		try (Jedis jedis = jedisPool.getResource()) {
			if (!jedis.exists(bucketKey)) {
				initializeBucket(key);
			}

			// Lua script
			String script = "local tokens = redis.call('HGET', KEYS[1], 'tokens') "
					+ "local lastRefillTime = redis.call('HGET', KEYS[1], 'lastRefillTime') "
					+ "local currentTime = tonumber(ARGV[1]) " + "local capacity = tonumber(ARGV[2]) "
					+ "local refillTokens = tonumber(ARGV[3]) " + "local refillIntervalInMillis = tonumber(ARGV[4]) "
					+ "local elapsed = currentTime - tonumber(lastRefillTime) "
					+ "local tokensToAdd = math.floor(elapsed / refillIntervalInMillis) * refillTokens "
					+ "if tokensToAdd > 0 then " + "    tokens = math.min(capacity, tonumber(tokens) + tokensToAdd) "
					+ "    redis.call('HSET', KEYS[1], 'tokens', tokens) "
					+ "    redis.call('HSET', KEYS[1], 'lastRefillTime', currentTime) " + "end "
					+ "if tonumber(tokens) > 0 then " + "    redis.call('HINCRBY', KEYS[1], 'tokens', -1) "
					+ "    return 1 " + "else " + "    return 0 " + "end";

			Object result = jedis.eval(script, 1, bucketKey, String.valueOf(System.currentTimeMillis()),
					String.valueOf(capacity), String.valueOf(refillTokens), String.valueOf(refillIntervalInMillis));
			return result.equals(1L);
		}
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

	public void destroy() {
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
