/*
 * Copyright (C) 2008, 2015 Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class NBTest {
	@Test
	public void testCompareUInt32() {
		assertTrue(NB.compareUInt32(0, 0) == 0);
		assertTrue(NB.compareUInt32(1, 0) > 0);
		assertTrue(NB.compareUInt32(0, 1) < 0);
		assertTrue(NB.compareUInt32(-1, 0) > 0);
		assertTrue(NB.compareUInt32(0, -1) < 0);
		assertTrue(NB.compareUInt32(-1, 1) > 0);
		assertTrue(NB.compareUInt32(1, -1) < 0);
	}

	@Test
	public void testCompareUInt64() {
		assertTrue(NB.compareUInt64(0, 0) == 0);
		assertTrue(NB.compareUInt64(1, 0) > 0);
		assertTrue(NB.compareUInt64(0, 1) < 0);
		assertTrue(NB.compareUInt64(-1, 0) > 0);
		assertTrue(NB.compareUInt64(0, -1) < 0);
		assertTrue(NB.compareUInt64(-1, 1) > 0);
		assertTrue(NB.compareUInt64(1, -1) < 0);
	}

	@Test
	public void testDecodeUInt16() {
		assertEquals(0, NB.decodeUInt16(b(0, 0), 0));
		assertEquals(0, NB.decodeUInt16(padb(3, 0, 0), 3));

		assertEquals(3, NB.decodeUInt16(b(0, 3), 0));
		assertEquals(3, NB.decodeUInt16(padb(3, 0, 3), 3));

		assertEquals(0xde03, NB.decodeUInt16(b(0xde, 3), 0));
		assertEquals(0xde03, NB.decodeUInt16(padb(3, 0xde, 3), 3));

		assertEquals(0x03de, NB.decodeUInt16(b(3, 0xde), 0));
		assertEquals(0x03de, NB.decodeUInt16(padb(3, 3, 0xde), 3));

		assertEquals(0xffff, NB.decodeUInt16(b(0xff, 0xff), 0));
		assertEquals(0xffff, NB.decodeUInt16(padb(3, 0xff, 0xff), 3));
	}

	@Test
	public void testDecodeUInt24() {
		assertEquals(0, NB.decodeUInt24(b(0, 0, 0), 0));
		assertEquals(0, NB.decodeUInt24(padb(3, 0, 0, 0), 3));

		assertEquals(3, NB.decodeUInt24(b(0, 0, 3), 0));
		assertEquals(3, NB.decodeUInt24(padb(3, 0, 0, 3), 3));

		assertEquals(0xcede03, NB.decodeUInt24(b(0xce, 0xde, 3), 0));
		assertEquals(0xbade03, NB.decodeUInt24(padb(3, 0xba, 0xde, 3), 3));

		assertEquals(0x03bade, NB.decodeUInt24(b(3, 0xba, 0xde), 0));
		assertEquals(0x03bade, NB.decodeUInt24(padb(3, 3, 0xba, 0xde), 3));

		assertEquals(0xffffff, NB.decodeUInt24(b(0xff, 0xff, 0xff), 0));
		assertEquals(0xffffff, NB.decodeUInt24(padb(3, 0xff, 0xff, 0xff), 3));
	}

	@Test
	public void testDecodeInt32() {
		assertEquals(0, NB.decodeInt32(b(0, 0, 0, 0), 0));
		assertEquals(0, NB.decodeInt32(padb(3, 0, 0, 0, 0), 3));

		assertEquals(3, NB.decodeInt32(b(0, 0, 0, 3), 0));
		assertEquals(3, NB.decodeInt32(padb(3, 0, 0, 0, 3), 3));

		assertEquals(0xdeadbeef, NB.decodeInt32(b(0xde, 0xad, 0xbe, 0xef), 0));
		assertEquals(0xdeadbeef, NB.decodeInt32(
				padb(3, 0xde, 0xad, 0xbe, 0xef), 3));

		assertEquals(0x0310adef, NB.decodeInt32(b(0x03, 0x10, 0xad, 0xef), 0));
		assertEquals(0x0310adef, NB.decodeInt32(
				padb(3, 0x03, 0x10, 0xad, 0xef), 3));

		assertEquals(0xffffffff, NB.decodeInt32(b(0xff, 0xff, 0xff, 0xff), 0));
		assertEquals(0xffffffff, NB.decodeInt32(
				padb(3, 0xff, 0xff, 0xff, 0xff), 3));
	}

	@Test
	public void testDecodeUInt32() {
		assertEquals(0L, NB.decodeUInt32(b(0, 0, 0, 0), 0));
		assertEquals(0L, NB.decodeUInt32(padb(3, 0, 0, 0, 0), 3));

		assertEquals(3L, NB.decodeUInt32(b(0, 0, 0, 3), 0));
		assertEquals(3L, NB.decodeUInt32(padb(3, 0, 0, 0, 3), 3));

		assertEquals(0xdeadbeefL, NB.decodeUInt32(b(0xde, 0xad, 0xbe, 0xef), 0));
		assertEquals(0xdeadbeefL, NB.decodeUInt32(padb(3, 0xde, 0xad, 0xbe,
				0xef), 3));

		assertEquals(0x0310adefL, NB.decodeUInt32(b(0x03, 0x10, 0xad, 0xef), 0));
		assertEquals(0x0310adefL, NB.decodeUInt32(padb(3, 0x03, 0x10, 0xad,
				0xef), 3));

		assertEquals(0xffffffffL, NB.decodeUInt32(b(0xff, 0xff, 0xff, 0xff), 0));
		assertEquals(0xffffffffL, NB.decodeUInt32(padb(3, 0xff, 0xff, 0xff,
				0xff), 3));
	}

	@Test
	public void testDecodeUInt64() {
		assertEquals(0L, NB.decodeUInt64(b(0, 0, 0, 0, 0, 0, 0, 0), 0));
		assertEquals(0L, NB.decodeUInt64(padb(3, 0, 0, 0, 0, 0, 0, 0, 0), 3));

		assertEquals(3L, NB.decodeUInt64(b(0, 0, 0, 0, 0, 0, 0, 3), 0));
		assertEquals(3L, NB.decodeUInt64(padb(3, 0, 0, 0, 0, 0, 0, 0, 3), 3));

		assertEquals(0xdeadbeefL, NB.decodeUInt64(b(0, 0, 0, 0, 0xde, 0xad,
				0xbe, 0xef), 0));
		assertEquals(0xdeadbeefL, NB.decodeUInt64(padb(3, 0, 0, 0, 0, 0xde,
				0xad, 0xbe, 0xef), 3));

		assertEquals(0x0310adefL, NB.decodeUInt64(b(0, 0, 0, 0, 0x03, 0x10,
				0xad, 0xef), 0));
		assertEquals(0x0310adefL, NB.decodeUInt64(padb(3, 0, 0, 0, 0, 0x03,
				0x10, 0xad, 0xef), 3));

		assertEquals(0xc0ffee78deadbeefL, NB.decodeUInt64(b(0xc0, 0xff, 0xee,
				0x78, 0xde, 0xad, 0xbe, 0xef), 0));
		assertEquals(0xc0ffee78deadbeefL, NB.decodeUInt64(padb(3, 0xc0, 0xff,
				0xee, 0x78, 0xde, 0xad, 0xbe, 0xef), 3));

		assertEquals(0x00000000ffffffffL, NB.decodeUInt64(b(0, 0, 0, 0, 0xff,
				0xff, 0xff, 0xff), 0));
		assertEquals(0x00000000ffffffffL, NB.decodeUInt64(padb(3, 0, 0, 0, 0,
				0xff, 0xff, 0xff, 0xff), 3));
		assertEquals(0xffffffffffffffffL, NB.decodeUInt64(b(0xff, 0xff, 0xff,
				0xff, 0xff, 0xff, 0xff, 0xff), 0));
		assertEquals(0xffffffffffffffffL, NB.decodeUInt64(padb(3, 0xff, 0xff,
				0xff, 0xff, 0xff, 0xff, 0xff, 0xff), 3));
	}

	@Test
	public void testEncodeInt16() {
		final byte[] out = new byte[16];

		prepareOutput(out);
		NB.encodeInt16(out, 0, 0);
		assertOutput(b(0, 0), out, 0);

		prepareOutput(out);
		NB.encodeInt16(out, 3, 0);
		assertOutput(b(0, 0), out, 3);

		prepareOutput(out);
		NB.encodeInt16(out, 0, 3);
		assertOutput(b(0, 3), out, 0);

		prepareOutput(out);
		NB.encodeInt16(out, 3, 3);
		assertOutput(b(0, 3), out, 3);

		prepareOutput(out);
		NB.encodeInt16(out, 0, 0xdeac);
		assertOutput(b(0xde, 0xac), out, 0);

		prepareOutput(out);
		NB.encodeInt16(out, 3, 0xdeac);
		assertOutput(b(0xde, 0xac), out, 3);

		prepareOutput(out);
		NB.encodeInt16(out, 3, -1);
		assertOutput(b(0xff, 0xff), out, 3);
	}

	@Test
	public void testEncodeInt24() {
		byte[] out = new byte[16];

		prepareOutput(out);
		NB.encodeInt24(out, 0, 0);
		assertOutput(b(0, 0, 0), out, 0);

		prepareOutput(out);
		NB.encodeInt24(out, 3, 0);
		assertOutput(b(0, 0, 0), out, 3);

		prepareOutput(out);
		NB.encodeInt24(out, 0, 3);
		assertOutput(b(0, 0, 3), out, 0);

		prepareOutput(out);
		NB.encodeInt24(out, 3, 3);
		assertOutput(b(0, 0, 3), out, 3);

		prepareOutput(out);
		NB.encodeInt24(out, 0, 0xc0deac);
		assertOutput(b(0xc0, 0xde, 0xac), out, 0);

		prepareOutput(out);
		NB.encodeInt24(out, 3, 0xbadeac);
		assertOutput(b(0xba, 0xde, 0xac), out, 3);

		prepareOutput(out);
		NB.encodeInt24(out, 3, -1);
		assertOutput(b(0xff, 0xff, 0xff), out, 3);
	}

	@Test
	public void testEncodeInt32() {
		final byte[] out = new byte[16];

		prepareOutput(out);
		NB.encodeInt32(out, 0, 0);
		assertOutput(b(0, 0, 0, 0), out, 0);

		prepareOutput(out);
		NB.encodeInt32(out, 3, 0);
		assertOutput(b(0, 0, 0, 0), out, 3);

		prepareOutput(out);
		NB.encodeInt32(out, 0, 3);
		assertOutput(b(0, 0, 0, 3), out, 0);

		prepareOutput(out);
		NB.encodeInt32(out, 3, 3);
		assertOutput(b(0, 0, 0, 3), out, 3);

		prepareOutput(out);
		NB.encodeInt32(out, 0, 0xdeac);
		assertOutput(b(0, 0, 0xde, 0xac), out, 0);

		prepareOutput(out);
		NB.encodeInt32(out, 3, 0xdeac);
		assertOutput(b(0, 0, 0xde, 0xac), out, 3);

		prepareOutput(out);
		NB.encodeInt32(out, 0, 0xdeac9853);
		assertOutput(b(0xde, 0xac, 0x98, 0x53), out, 0);

		prepareOutput(out);
		NB.encodeInt32(out, 3, 0xdeac9853);
		assertOutput(b(0xde, 0xac, 0x98, 0x53), out, 3);

		prepareOutput(out);
		NB.encodeInt32(out, 3, -1);
		assertOutput(b(0xff, 0xff, 0xff, 0xff), out, 3);
	}

	@Test
	public void testEncodeInt64() {
		final byte[] out = new byte[16];

		prepareOutput(out);
		NB.encodeInt64(out, 0, 0L);
		assertOutput(b(0, 0, 0, 0, 0, 0, 0, 0), out, 0);

		prepareOutput(out);
		NB.encodeInt64(out, 3, 0L);
		assertOutput(b(0, 0, 0, 0, 0, 0, 0, 0), out, 3);

		prepareOutput(out);
		NB.encodeInt64(out, 0, 3L);
		assertOutput(b(0, 0, 0, 0, 0, 0, 0, 3), out, 0);

		prepareOutput(out);
		NB.encodeInt64(out, 3, 3L);
		assertOutput(b(0, 0, 0, 0, 0, 0, 0, 3), out, 3);

		prepareOutput(out);
		NB.encodeInt64(out, 0, 0xdeacL);
		assertOutput(b(0, 0, 0, 0, 0, 0, 0xde, 0xac), out, 0);

		prepareOutput(out);
		NB.encodeInt64(out, 3, 0xdeacL);
		assertOutput(b(0, 0, 0, 0, 0, 0, 0xde, 0xac), out, 3);

		prepareOutput(out);
		NB.encodeInt64(out, 0, 0xdeac9853L);
		assertOutput(b(0, 0, 0, 0, 0xde, 0xac, 0x98, 0x53), out, 0);

		prepareOutput(out);
		NB.encodeInt64(out, 3, 0xdeac9853L);
		assertOutput(b(0, 0, 0, 0, 0xde, 0xac, 0x98, 0x53), out, 3);

		prepareOutput(out);
		NB.encodeInt64(out, 0, 0xac431242deac9853L);
		assertOutput(b(0xac, 0x43, 0x12, 0x42, 0xde, 0xac, 0x98, 0x53), out, 0);

		prepareOutput(out);
		NB.encodeInt64(out, 3, 0xac431242deac9853L);
		assertOutput(b(0xac, 0x43, 0x12, 0x42, 0xde, 0xac, 0x98, 0x53), out, 3);

		prepareOutput(out);
		NB.encodeInt64(out, 3, -1L);
		assertOutput(b(0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff), out, 3);
	}

	private static void prepareOutput(byte[] buf) {
		for (int i = 0; i < buf.length; i++)
			buf[i] = (byte) (0x77 + i);
	}

	private static void assertOutput(final byte[] expect, final byte[] buf,
			final int offset) {
		for (int i = 0; i < offset; i++)
			assertEquals((byte) (0x77 + i), buf[i]);
		for (int i = 0; i < expect.length; i++)
			assertEquals(expect[i], buf[offset + i]);
		for (int i = offset + expect.length; i < buf.length; i++)
			assertEquals((byte) (0x77 + i), buf[i]);
	}

	private static byte[] b(int a, int b) {
		return new byte[] { (byte) a, (byte) b };
	}

	private static byte[] padb(int len, int a, int b) {
		final byte[] r = new byte[len + 2];
		for (int i = 0; i < len; i++)
			r[i] = (byte) 0xaf;
		r[len] = (byte) a;
		r[len + 1] = (byte) b;
		return r;
	}

	private static byte[] b(int a, int b, int c) {
		return new byte[] { (byte) a, (byte) b, (byte) c };
	}

	private static byte[] b(int a, int b, int c, int d) {
		return new byte[] { (byte) a, (byte) b, (byte) c, (byte) d };
	}

	private static byte[] padb(int len, int a, int b, int c) {
		final byte[] r = new byte[len + 4];
		for (int i = 0; i < len; i++)
			r[i] = (byte) 0xaf;
		r[len] = (byte) a;
		r[len + 1] = (byte) b;
		r[len + 2] = (byte) c;
		return r;
	}

	private static byte[] padb(final int len, final int a, final int b,
			final int c, final int d) {
		final byte[] r = new byte[len + 4];
		for (int i = 0; i < len; i++)
			r[i] = (byte) 0xaf;
		r[len] = (byte) a;
		r[len + 1] = (byte) b;
		r[len + 2] = (byte) c;
		r[len + 3] = (byte) d;
		return r;
	}

	private static byte[] b(final int a, final int b, final int c, final int d,
			final int e, final int f, final int g, final int h) {
		return new byte[] { (byte) a, (byte) b, (byte) c, (byte) d, (byte) e,
				(byte) f, (byte) g, (byte) h };
	}

	private static byte[] padb(final int len, final int a, final int b,
			final int c, final int d, final int e, final int f, final int g,
			final int h) {
		final byte[] r = new byte[len + 8];
		for (int i = 0; i < len; i++)
			r[i] = (byte) 0xaf;
		r[len] = (byte) a;
		r[len + 1] = (byte) b;
		r[len + 2] = (byte) c;
		r[len + 3] = (byte) d;
		r[len + 4] = (byte) e;
		r[len + 5] = (byte) f;
		r[len + 6] = (byte) g;
		r[len + 7] = (byte) h;
		return r;
	}
}
