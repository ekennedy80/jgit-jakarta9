/*
 * Copyright (C) 2009, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LongMapTest {
	private LongMap<Long> map;

	@BeforeEach
	public void setUp() throws Exception {
		map = new LongMap<>();
	}

	@Test
	public void testEmptyMap() {
		assertFalse(map.containsKey(0));
		assertFalse(map.containsKey(1));

		assertNull(map.get(0));
		assertNull(map.get(1));

		assertNull(map.remove(0));
		assertNull(map.remove(1));
	}

	@Test
	public void testInsertMinValue() {
		final Long min = Long.valueOf(Long.MIN_VALUE);
		assertNull(map.put(Long.MIN_VALUE, min));
		assertTrue(map.containsKey(Long.MIN_VALUE));
		assertSame(min, map.get(Long.MIN_VALUE));
		assertFalse(map.containsKey(Integer.MIN_VALUE));
	}

	@Test
	public void testReplaceMaxValue() {
		final Long min = Long.valueOf(Long.MAX_VALUE);
		final Long one = Long.valueOf(1);
		assertNull(map.put(Long.MAX_VALUE, min));
		assertSame(min, map.get(Long.MAX_VALUE));
		assertSame(min, map.put(Long.MAX_VALUE, one));
		assertSame(one, map.get(Long.MAX_VALUE));
	}

	@Test
	public void testRemoveOne() {
		final long start = 1;
		assertNull(map.put(start, Long.valueOf(start)));
		assertEquals(Long.valueOf(start), map.remove(start));
		assertFalse(map.containsKey(start));
	}

	@Test
	public void testRemoveCollision1() {
		// This test relies upon the fact that we always >>> 1 the value
		// to derive an unsigned hash code. Thus, 0 and 1 fall into the
		// same hash bucket. Further it relies on the fact that we add
		// the 2nd put at the top of the chain, so removing the 1st will
		// cause a different code path.
		//
		assertNull(map.put(0, Long.valueOf(0)));
		assertNull(map.put(1, Long.valueOf(1)));
		assertEquals(Long.valueOf(0), map.remove(0));

		assertFalse(map.containsKey(0));
		assertTrue(map.containsKey(1));
	}

	@Test
	public void testRemoveCollision2() {
		// This test relies upon the fact that we always >>> 1 the value
		// to derive an unsigned hash code. Thus, 0 and 1 fall into the
		// same hash bucket. Further it relies on the fact that we add
		// the 2nd put at the top of the chain, so removing the 2nd will
		// cause a different code path.
		//
		assertNull(map.put(0, Long.valueOf(0)));
		assertNull(map.put(1, Long.valueOf(1)));
		assertEquals(Long.valueOf(1), map.remove(1));

		assertTrue(map.containsKey(0));
		assertFalse(map.containsKey(1));
	}

	@Test
	public void testSmallMap() {
		final long start = 12;
		final long n = 8;
		for (long i = start; i < start + n; i++)
			assertNull(map.put(i, Long.valueOf(i)));
		for (long i = start; i < start + n; i++)
			assertEquals(Long.valueOf(i), map.get(i));
	}

	@Test
	public void testLargeMap() {
		final long start = Integer.MAX_VALUE;
		final long n = 100000;
		for (long i = start; i < start + n; i++)
			assertNull(map.put(i, Long.valueOf(i)));
		for (long i = start; i < start + n; i++)
			assertEquals(Long.valueOf(i), map.get(i));
	}
}
