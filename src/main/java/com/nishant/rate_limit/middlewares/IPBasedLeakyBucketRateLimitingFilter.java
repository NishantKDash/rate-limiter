package com.nishant.rate_limit.middlewares;

import java.io.IOException;

import com.nishant.rate_limit.models.leakingBucket.Request;
import com.nishant.rate_limit.services.LeakingBucketRateLimitingService;
import com.nishant.rate_limit.services.TokenBucketRateLimitingService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class IPBasedLeakyBucketRateLimitingFilter implements RateLimitingFilter {

	private LeakingBucketRateLimitingService rateLimitingService;

	public IPBasedLeakyBucketRateLimitingFilter(LeakingBucketRateLimitingService rateLimitingService) {
		this.rateLimitingService = rateLimitingService;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		String ip = httpRequest.getRemoteAddr();

		if (this.rateLimitingService.consume(ip, request, response)) {
			while (rateLimitingService.getCurrentSize() > 0)
			{
				Request requestToBeProcessed = rateLimitingService.processRequests();
				process(chain, requestToBeProcessed.getServletRequest(), requestToBeProcessed.getServletResponse());
			}
		}

		else {
			httpResponse.setStatus(429);
			httpResponse.setIntHeader("X-Ratelimit-Limit", this.rateLimitingService.getCapacity());
			httpResponse.setHeader("X-Ratelimit-Retry-After",
					String.valueOf(this.rateLimitingService.getInterval() / 1000));
		}

	}

	public void process(FilterChain chain, ServletRequest request, ServletResponse response)
			throws IOException, ServletException {
		chain.doFilter(request, response);
	}

}
