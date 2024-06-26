/*
 * Copyright (C) 2012, Robin Rosenberg and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.junit;

/**
 * Assertion class
 */
public class Assert {

	/**
	 * Assert booleans are equal
	 *
	 * @param expect
	 *            expected value
	 * @param actual
	 *            actual value
	 */
	public static void assertEquals(boolean expect, boolean actual) {
		assertEquals(Boolean.valueOf(expect),
				Boolean.valueOf(actual));
	}

	/**
	 * Assert booleans are equal
	 *
	 * @param message
	 *            message
	 * @param expect
	 *            expected value
	 * @param actual
	 *            actual value
	 */
	public static void assertEquals(String message, boolean expect,
			boolean actual) {
		assertEquals(message, Boolean.valueOf(expect),
						Boolean.valueOf(actual));
	}
}
