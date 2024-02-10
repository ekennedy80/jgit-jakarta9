/*
 * Copyright (C) 2017 Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.junit;

import org.eclipse.jgit.lib.ProgressMonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Strict work monitor
 */
public final class StrictWorkMonitor implements ProgressMonitor {
	private int lastWork;
	private int totalWork;

	@Override
	public void start(int totalTasks) {
		// empty
	}

	@Override
	public void beginTask(String title, int total) {
		this.totalWork = total;
		lastWork = 0;
	}

	@Override
	public void update(int completed) {
		lastWork += completed;
	}

	@Override
	public void endTask() {
		assertEquals(totalWork, lastWork);
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public void showDuration(boolean enabled) {
		// not implemented
	}
}
