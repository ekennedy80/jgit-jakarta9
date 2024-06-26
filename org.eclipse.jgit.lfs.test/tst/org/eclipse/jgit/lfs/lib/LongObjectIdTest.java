/*
 * Copyright (C) 2015, Matthias Sohn <matthias.sohn@sap.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.lfs.lib;

import org.eclipse.jgit.lfs.errors.InvalidLongObjectIdException;
import org.eclipse.jgit.lfs.test.LongObjectIdTestUtils;
import org.eclipse.jgit.util.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/*
 * Ported to SHA-256 from org.eclipse.jgit.lib.ObjectIdTest
 */
public class LongObjectIdTest {
	private static Path tmp;

	@BeforeEach
	public void setup(TestInfo testInfo) throws IOException {
		tmp = Files.createTempDirectory("jgit_test_");
		System.out.println(testInfo);
	}

	@AfterEach
	public void tearDown() throws IOException {
		FileUtils.delete(tmp.toFile(), FileUtils.RECURSIVE | FileUtils.RETRY);
	}

	@Test
	public void test001_toString() {
		final String x = "8367b0edc81df80e6b42eb1b71f783111224e058cb3da37894d065d2deb7ab0a";
		final LongObjectId oid = LongObjectId.fromString(x);
		assertEquals(x, oid.name());
	}

	@Test
	public void test002_toString() {
		final String x = "140ce71d628cceb78e3709940ba52a651a0c4a9c1400f2e15e998a1a43887edf";
		final LongObjectId oid = LongObjectId.fromString(x);
		assertEquals(x, oid.name());
	}

	@Test
	public void test003_equals() {
		final String x = "8367b0edc81df80e6b42eb1b71f783111224e058cb3da37894d065d2deb7ab0a";
		final LongObjectId a = LongObjectId.fromString(x);
		final LongObjectId b = LongObjectId.fromString(x);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(b, a);
	}

	@Test
	public void test004_isId() {
		assertTrue(LongObjectId.isId(
				"8367b0edc81df80e6b42eb1b71f783111224e058cb3da37894d065d2deb7ab0a"));
	}

	@Test
	public void test005_notIsId() {
		assertFalse(LongObjectId.isId("bob"));
	}

	@Test
	public void test006_notIsId() {
		assertFalse(LongObjectId.isId(
				"8367b0edc81df80e6b42eb1b71f783111224e058cb3da37894d065d2deb7ab0"));
	}

	@Test
	public void test007_isId() {
		assertTrue(LongObjectId.isId(
				"8367b0edc81df80e6b42eb1b71f783111224e058cb3da37894d065d2dEb7ab0A"));
	}

	@Test
	public void test008_notIsId() {
		assertFalse(LongObjectId.isId(
				"g367b0edc81df80e6b42eb1b71f783111224e058cb3da37894d065d2deb7ab0a"));
	}

	@Test
	public void test009_toString() {
		final String x = "140ce71d628cceb78e3709940ba52a651a0c4a9c1400f2e15e998a1a43887edf";
		final LongObjectId oid = LongObjectId.fromString(x);
		assertEquals(x, LongObjectId.toString(oid));
	}

	@Test
	public void test010_toString() {
		final String x = "0000000000000000000000000000000000000000000000000000000000000000";
		assertEquals(x, LongObjectId.toString(null));
	}

	@Test
	public void test011_toString() {
		final String x = "0123456789ABCDEFabcdef01234567890123456789ABCDEFabcdef0123456789";
		final LongObjectId oid = LongObjectId.fromString(x);
		assertEquals(x.toLowerCase(Locale.ROOT), oid.name());
	}

	@Test
	public void testGetByte() {
		byte[] raw = new byte[32];
		for (int i = 0; i < 32; i++)
			raw[i] = (byte) (0xa0 + i);
		LongObjectId id = LongObjectId.fromRaw(raw);

		assertEquals(raw[0] & 0xff, id.getFirstByte());
		assertEquals(raw[0] & 0xff, id.getByte(0));
		assertEquals(raw[1] & 0xff, id.getByte(1));
		assertEquals(raw[1] & 0xff, id.getSecondByte());

		for (int i = 2; i < 32; i++) {
			assertEquals(raw[i] & 0xff, id.getByte(i));
		}
		try {
			id.getByte(32);
			fail("LongObjectId has 32 byte only");
		} catch (ArrayIndexOutOfBoundsException e) {
			// expected
		}
	}

	@Test
	public void testSetByte() {
		byte[] exp = new byte[32];
		for (int i = 0; i < 32; i++) {
			exp[i] = (byte) (0xa0 + i);
		}

		MutableLongObjectId id = new MutableLongObjectId();
		id.fromRaw(exp);
		assertEquals(LongObjectId.fromRaw(exp).name(), id.name());

		id.setByte(0, 0x10);
		assertEquals(0x10, id.getByte(0));
		exp[0] = 0x10;
		assertEquals(LongObjectId.fromRaw(exp).name(), id.name());

		for (int p = 1; p < 32; p++) {
			id.setByte(p, 0x10 + p);
			assertEquals(0x10 + p, id.getByte(p));
			exp[p] = (byte) (0x10 + p);
			assertEquals(LongObjectId.fromRaw(exp).name(), id.name());
		}

		for (int p = 0; p < 32; p++) {
			id.setByte(p, 0x80 + p);
			assertEquals(0x80 + p, id.getByte(p));
			exp[p] = (byte) (0x80 + p);
			assertEquals(LongObjectId.fromRaw(exp).name(), id.name());
		}
	}

	@Test
	public void testZeroId() {
		AnyLongObjectId zero = new LongObjectId(0L, 0L, 0L, 0L);
		assertEquals(zero, LongObjectId.zeroId());
		assertEquals(
				"0000000000000000000000000000000000000000000000000000000000000000",
				LongObjectId.zeroId().name());
	}

	@Test
	public void testEquals() {
		AnyLongObjectId id1 = LongObjectIdTestUtils.hash("test");
		assertTrue(id1.equals(id1));
		AnyLongObjectId id2 = new LongObjectId(id1);
		assertEquals( id1, id2);

		id2 = LongObjectIdTestUtils.hash("other");
		assertNotEquals(id1, id2);
	}

	@Test
	public void testCopyRawBytes() {
		AnyLongObjectId id1 = LongObjectIdTestUtils.hash("test");
		AnyLongObjectId id2 = new LongObjectId(id1);

		byte[] buf = new byte[64];
		id1.copyRawTo(buf, 0);
		id2.copyRawTo(buf, 32);
		assertTrue(LongObjectId.equals(buf, 0, buf, 32));
	}

	@Test
	public void testCopyRawLongs() {
		long[] a = new long[4];
		a[0] = 1L;
		a[1] = 2L;
		a[2] = 3L;
		a[3] = 4L;
		AnyLongObjectId id1 = new LongObjectId(a[0], a[1], a[2], a[3]);
		AnyLongObjectId id2 = LongObjectId.fromRaw(a);
		assertEquals(id1, id2);
	}

	@Test
	public void testCopyFromStringInvalid() {
		AnyLongObjectId id1 = LongObjectIdTestUtils.hash("test");
		try {
			LongObjectId.fromString(id1.name() + "01234");
			fail("expected InvalidLongObjectIdException");
		} catch (InvalidLongObjectIdException e) {
			assertEquals("Invalid id: " + id1.name() + "01234",
					e.getMessage());
		}
	}

	@Test
	public void testCopyFromStringByte() {
		AnyLongObjectId id1 = LongObjectIdTestUtils.hash("test");
		byte[] buf = new byte[64];
		Charset cs = US_ASCII;
		cs.encode(id1.name()).get(buf);
		AnyLongObjectId id2 = LongObjectId.fromString(buf, 0);
		assertEquals(id1, id2);
	}

//	@Test
//	public void testHashFile() throws IOException {
//		AnyLongObjectId id1 = LongObjectIdTestUtils.hash("test");
//		Path f = tmp.resolve("test");
//		JGitTestUtil.write(f.toFile(), "test");
//		AnyLongObjectId id2 = LongObjectIdTestUtils.hash(f);
//		assertEquals(id1, id2);
//	}

	@Test
	public void testCompareTo() {
		AnyLongObjectId id1 = LongObjectId.fromString(
				"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
		assertEquals(0, id1.compareTo(LongObjectId.fromString(
				"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")));
		AnyLongObjectId self = id1;
		assertEquals(0, id1.compareTo(self));

		assertEquals(-1, id1.compareTo(LongObjectId.fromString(
				"1123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")));
		assertEquals(-1, id1.compareTo(LongObjectId.fromString(
				"0123456789abcdef1123456789abcdef0123456789abcdef0123456789abcdef")));
		assertEquals(-1, id1.compareTo(LongObjectId.fromString(
				"0123456789abcdef0123456789abcdef1123456789abcdef0123456789abcdef")));
		assertEquals(-1, id1.compareTo(LongObjectId.fromString(
				"0123456789abcdef0123456789abcdef0123456789abcdef1123456789abcdef")));

		assertEquals(1, id1.compareTo(LongObjectId.fromString(
				"0023456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")));
		assertEquals(1, id1.compareTo(LongObjectId.fromString(
				"0123456789abcdef0023456789abcdef0123456789abcdef0123456789abcdef")));
		assertEquals(1, id1.compareTo(LongObjectId.fromString(
				"0123456789abcdef0123456789abcdef0023456789abcdef0123456789abcdef")));
		assertEquals(1, id1.compareTo(LongObjectId.fromString(
				"0123456789abcdef0123456789abcdef0123456789abcdef0023456789abcdef")));
	}

	@Test
	public void testCompareToByte() {
		AnyLongObjectId id1 = LongObjectId.fromString(
				"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
		byte[] buf = new byte[32];
		id1.copyRawTo(buf, 0);
		assertEquals(0, id1.compareTo(buf, 0));

		LongObjectId
				.fromString(
						"1123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
				.copyRawTo(buf, 0);
		assertEquals(-1, id1.compareTo(buf, 0));

		LongObjectId
				.fromString(
						"0023456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
				.copyRawTo(buf, 0);
		assertEquals(1, id1.compareTo(buf, 0));
	}

	@Test
	public void testCompareToLong() {
		AnyLongObjectId id1 = new LongObjectId(1L, 2L, 3L, 4L);
		long[] buf = new long[4];
		id1.copyRawTo(buf, 0);
		assertEquals(0, id1.compareTo(buf, 0));

		new LongObjectId(2L, 2L, 3L, 4L).copyRawTo(buf, 0);
		assertEquals(-1, id1.compareTo(buf, 0));

		new LongObjectId(0L, 2L, 3L, 4L).copyRawTo(buf, 0);
		assertEquals(1, id1.compareTo(buf, 0));
	}

	@Test
	public void testCopyToByte() {
		AnyLongObjectId id1 = LongObjectIdTestUtils.hash("test");
		byte[] buf = new byte[64];
		id1.copyTo(buf, 0);
		assertEquals(id1, LongObjectId.fromString(buf, 0));
	}

	@Test
	public void testCopyRawToByteBuffer() {
		AnyLongObjectId id1 = LongObjectIdTestUtils.hash("test");
		ByteBuffer buf = ByteBuffer.allocate(32);
		id1.copyRawTo(buf);
		assertEquals(id1, LongObjectId.fromRaw(buf.array(), 0));
	}

	@Test
	public void testCopyToByteBuffer() {
		AnyLongObjectId id1 = LongObjectIdTestUtils.hash("test");
		ByteBuffer buf = ByteBuffer.allocate(64);
		id1.copyTo(buf);
		assertEquals(id1, LongObjectId.fromString(buf.array(), 0));
	}

	@Test
	public void testCopyRawToOutputStream() throws IOException {
		AnyLongObjectId id1 = LongObjectIdTestUtils.hash("test");
		ByteArrayOutputStream os = new ByteArrayOutputStream(32);
		id1.copyRawTo(os);
		assertEquals(id1, LongObjectId.fromRaw(os.toByteArray(), 0));
	}

	@Test
	public void testCopyToOutputStream() throws IOException {
		AnyLongObjectId id1 = LongObjectIdTestUtils.hash("test");
		ByteArrayOutputStream os = new ByteArrayOutputStream(64);
		id1.copyTo(os);
		assertEquals(id1, LongObjectId.fromString(os.toByteArray(), 0));
	}

	@Test
	public void testCopyToWriter() throws IOException {
		AnyLongObjectId id1 = LongObjectIdTestUtils.hash("test");
		ByteArrayOutputStream os = new ByteArrayOutputStream(64);
		try (OutputStreamWriter w = new OutputStreamWriter(os,
				UTF_8)) {
			id1.copyTo(w);
		}
		assertEquals(id1, LongObjectId.fromString(os.toByteArray(), 0));
	}

	@Test
	public void testCopyToWriterWithBuf() throws IOException {
		AnyLongObjectId id1 = LongObjectIdTestUtils.hash("test");
		ByteArrayOutputStream os = new ByteArrayOutputStream(64);
		try (OutputStreamWriter w = new OutputStreamWriter(os,
				UTF_8)) {
			char[] buf = new char[64];
			id1.copyTo(buf, w);
		}
		assertEquals(id1, LongObjectId.fromString(os.toByteArray(), 0));
	}

	@Test
	public void testCopyToStringBuilder() {
		AnyLongObjectId id1 = LongObjectIdTestUtils.hash("test");
		StringBuilder sb = new StringBuilder();
		char[] buf = new char[64];
		id1.copyTo(buf, sb);
		assertEquals(id1, LongObjectId.fromString(sb.toString()));
	}

	@Test
	public void testCopy() {
		AnyLongObjectId id1 = LongObjectIdTestUtils.hash("test");
		assertEquals(id1.copy(), id1);
		MutableLongObjectId id2 = new MutableLongObjectId();
		id2.fromObjectId(id1);
		assertEquals(id1, id2.copy());
	}
}
