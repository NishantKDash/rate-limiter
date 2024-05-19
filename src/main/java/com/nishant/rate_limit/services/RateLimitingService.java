package com.nishant.rate_limit.services;

public interface RateLimitingService {
   public boolean consume(String key);
   public int getCapacity();
   public long getInterval();
}
