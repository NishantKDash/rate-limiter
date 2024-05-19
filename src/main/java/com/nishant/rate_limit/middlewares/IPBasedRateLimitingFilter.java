package com.nishant.rate_limit.middlewares;

import java.io.IOException;

import com.nishant.rate_limit.services.RateLimitingService;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class IPBasedRateLimitingFilter implements RateLimitingFilter {

	private RateLimitingService rateLimitingService;

	public IPBasedRateLimitingFilter(RateLimitingService rateLimitingService) {
		this.rateLimitingService = rateLimitingService;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		 HttpServletRequest httpRequest = (HttpServletRequest) request;
	     HttpServletResponse httpResponse = (HttpServletResponse) response;
	     String ip = httpRequest.getRemoteAddr();
	     
	     if(this.rateLimitingService.consume(ip))
	    	 chain.doFilter(request, response);
	     else
	     {
	    	 httpResponse.setStatus(429);
	    	 httpResponse.setIntHeader("X-Ratelimit-Limit", this.rateLimitingService.getCapacity());
	    	 httpResponse.setHeader("X-Ratelimit-Retry-After", String.valueOf(this.rateLimitingService.getInterval() / 1000));
	     }
	}

}
