/*
 * Copyright (C) 2010, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.junit.http;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.Callback;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.concurrent.Semaphore;

/** Logs request made through {@link AppServer}. */
class TestRequestLog implements RequestLog, Handler {

	private static final int MAX = 16;

	private final List<AccessEvent> events = new ArrayList<>();

	private final Semaphore active = new Semaphore(MAX, true);

	/** Reset the log back to its original empty state. */
	void clear() {
		try {
			for (;;) {
				try {
					active.acquire(MAX);
					break;
				} catch (InterruptedException e) {
					continue;
				}
			}

			synchronized (events) {
				events.clear();
			}
		} finally {
			active.release(MAX);
		}
	}

	/** @return all of the events made since the last clear. */
	List<AccessEvent> getEvents() {
		try {
			for (;;) {
				try {
					active.acquire(MAX);
					break;
				} catch (InterruptedException e) {
					continue;
				}
			}

			synchronized (events) {
				return events;
			}
		} finally {
			active.release(MAX);
		}
	}

	@Override
	public void log(Request request, Response response) {

	}

	@Override
	public Server getServer() {
		return null;
	}

	@Override
	public void setServer(Server server) {

	}

	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {
		return false;
	}

	@Override
	public void destroy() {

	}

	@Override
	public void start() throws Exception {

	}

	@Override
	public void stop() throws Exception {

	}

	@Override
	public boolean isRunning() {
		return false;
	}

	@Override
	public boolean isStarted() {
		return false;
	}

	@Override
	public boolean isStarting() {
		return false;
	}

	@Override
	public boolean isStopping() {
		return false;
	}

	@Override
	public boolean isStopped() {
		return false;
	}

	@Override
	public boolean isFailed() {
		return false;
	}

	@Override
	public boolean addEventListener(EventListener eventListener) {
		return false;
	}

	@Override
	public boolean removeEventListener(EventListener eventListener) {
		return false;
	}
}
