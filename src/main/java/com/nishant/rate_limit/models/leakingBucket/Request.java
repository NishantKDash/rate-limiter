package com.nishant.rate_limit.models.leakingBucket;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

public class Request {
	private final ServletRequest request;
	private final ServletResponse response;

	public Request(ServletRequest request, ServletResponse response) {
		this.request = request;
		this.response = response;
	}

	public ServletRequest getServletRequest() {
		return this.request;
	}

	public ServletResponse getServletResponse() {
		return this.response;
	}
}
