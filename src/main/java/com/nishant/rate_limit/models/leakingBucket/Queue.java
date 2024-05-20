package com.nishant.rate_limit.models.leakingBucket;

import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

public class Queue {
	ConcurrentLinkedQueue<Request> queue;

	public Queue() {
		this.queue = new ConcurrentLinkedQueue<Request>();
	}

	public boolean insert(ServletRequest request, ServletResponse response) {
		try {
			this.queue.add(new Request(request, response));
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public Request process() {
		return this.queue.poll();
	}

	public int getSize() {
		return this.queue.size();
	}
}
