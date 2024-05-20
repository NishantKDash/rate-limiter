package com.nishant.rate_limit.services;

import java.time.Duration;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class DistributedTokenBucketRateLimitingService  {

	private final JedisPool jedisPool;
	private final int capacity;
	private final int refillTokens;
	private final long refillIntervalInMillis;

	public DistributedTokenBucketRateLimitingService(String connectionUrl, int capacity, int refillTokens,
			long refillIntervalInMillis) {
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(100);
		config.setMaxIdle(50);
		config.setMinIdle(10);
		config.setMaxWait(Duration.ofMillis(3000));
		config.setTestOnBorrow(true);
		config.setTestOnReturn(true);
		config.setTestWhileIdle(true);
		jedisPool = new JedisPool(config, connectionUrl);
		this.capacity = capacity;
		this.refillTokens = refillTokens;
		this.refillIntervalInMillis = refillIntervalInMillis;
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
		// TODO Auto-generated method stub
		return this.capacity;
	}

	public long getInterval() {
		// TODO Auto-generated method stub
		return this.refillIntervalInMillis;
	}

}
