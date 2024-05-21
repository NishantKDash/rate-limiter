package com.nishant.rate_limit.models.leakingBucket;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

public class Request {
	private final ServletRequest request;
	private final ServletResponse response;
	private final FilterChain chain;

	public Request(ServletRequest request, ServletResponse response, FilterChain chain) {
		this.request = request;
		this.response = response;
		this.chain = chain;
	}

	public ServletRequest getServletRequest() {
		return this.request;
	}

	public ServletResponse getServletResponse() {
		return this.response;
	}

	public FilterChain getFilterChain() {
		return this.chain;
	}
}
