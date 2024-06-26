/*
 * Copyright (C) 2010, Google Inc. and others
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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.junit.jupiter.api.Test;

public class RefListTest {
	private static final ObjectId ID = ObjectId
			.fromString("41eb0d88f833b558bddeb269b7ab77399cdf98ed");

	private static final Ref REF_A = newRef("A");

	private static final Ref REF_B = newRef("B");

	private static final Ref REF_c = newRef("c");

	@Test
	public void testEmpty() {
		RefList<Ref> list = RefList.emptyList();
		assertEquals(0, list.size());
		assertTrue(list.isEmpty());
		assertFalse(list.iterator().hasNext());
		assertEquals(-1, list.find("a"));
		assertEquals(-1, list.find("z"));
		assertFalse(list.contains("a"));
		assertNull(list.get("a"));
		try {
			list.get(0);
			fail("RefList.emptyList should have 0 element array");
		} catch (ArrayIndexOutOfBoundsException err) {
			// expected
		}
	}

	@Test
	public void testEmptyBuilder() {
		RefList<Ref> list = new RefList.Builder<>().toRefList();
		assertEquals(0, list.size());
		assertFalse(list.iterator().hasNext());
		assertEquals(-1, list.find("a"));
		assertEquals(-1, list.find("z"));
		assertFalse(list.contains("a"));
		assertNull(list.get("a"));
		assertTrue(list.asList().isEmpty());
		assertEquals("[]", list.toString());

		// default array capacity should be 16, with no bounds checking.
		assertNull(list.get(16 - 1));
		try {
			list.get(16);
			fail("default RefList should have 16 element array");
		} catch (ArrayIndexOutOfBoundsException err) {
			// expected
		}
	}

	@Test
	public void testBuilder_AddThenSort() {
		RefList.Builder<Ref> builder = new RefList.Builder<>(1);
		builder.add(REF_B);
		builder.add(REF_A);

		RefList<Ref> list = builder.toRefList();
		assertEquals(2, list.size());
		assertSame(REF_B, list.get(0));
		assertSame(REF_A, list.get(1));

		builder.sort();
		list = builder.toRefList();
		assertEquals(2, list.size());
		assertSame(REF_A, list.get(0));
		assertSame(REF_B, list.get(1));
	}

	@Test
	public void testBuilder_AddThenDedupe() {
		RefList.Builder<Ref> builder = new RefList.Builder<>(1);
		builder.add(REF_B);
		builder.add(REF_A);
		builder.add(REF_A);
		builder.add(REF_B);
		builder.add(REF_c);

		builder.sort();
		builder.dedupe((a, b) -> b);
		RefList<Ref> list = builder.toRefList();

		assertEquals(3, list.size());
		assertSame(REF_A, list.get(0));
		assertSame(REF_B, list.get(1));
		assertSame(REF_c, list.get(2));
	}

	@Test
	public void testBuilder_AddThenDedupe_Border() {
		RefList.Builder<Ref> builder = new RefList.Builder<>(1);
		builder.sort();
		builder.dedupe((a, b) -> b);
		RefList<Ref> list = builder.toRefList();
		assertTrue(list.isEmpty());

		builder = new RefList.Builder<>(1);
		builder.add(REF_A);
		builder.sort();
		builder.dedupe((a, b) -> b);
		list = builder.toRefList();
		assertEquals(1, list.size());
	}

	@Test
	public void testBuilder_AddAll() {
		RefList.Builder<Ref> builder = new RefList.Builder<>(1);
		Ref[] src = { REF_A, REF_B, REF_c, REF_A };
		builder.addAll(src, 1, 2);

		RefList<Ref> list = builder.toRefList();
		assertEquals(2, list.size());
		assertSame(REF_B, list.get(0));
		assertSame(REF_c, list.get(1));
	}

	@Test
	public void testBuilder_Set() {
		RefList.Builder<Ref> builder = new RefList.Builder<>();
		builder.add(REF_A);
		builder.add(REF_A);

		assertEquals(2, builder.size());
		assertSame(REF_A, builder.get(0));
		assertSame(REF_A, builder.get(1));

		RefList<Ref> list = builder.toRefList();
		assertEquals(2, list.size());
		assertSame(REF_A, list.get(0));
		assertSame(REF_A, list.get(1));
		builder.set(1, REF_B);

		list = builder.toRefList();
		assertEquals(2, list.size());
		assertSame(REF_A, list.get(0));
		assertSame(REF_B, list.get(1));
	}

	@Test
	public void testBuilder_Remove() {
		RefList.Builder<Ref> builder = new RefList.Builder<>();
		builder.add(REF_A);
		builder.add(REF_B);
		builder.remove(0);

		assertEquals(1, builder.size());
		assertSame(REF_B, builder.get(0));
	}

	@Test
	public void testSet() {
		RefList<Ref> one = toList(REF_A, REF_A);
		RefList<Ref> two = one.set(1, REF_B);
		assertNotSame(one, two);

		// one is not modified
		assertEquals(2, one.size());
		assertSame(REF_A, one.get(0));
		assertSame(REF_A, one.get(1));

		// but two is
		assertEquals(2, two.size());
		assertSame(REF_A, one.get(0));
		assertSame(REF_B, two.get(1));
	}

	@Test
	public void testAddToEmptyList() {
		RefList<Ref> one = toList();
		RefList<Ref> two = one.add(0, REF_B);
		assertNotSame(one, two);

		// one is not modified, but two is
		assertEquals(0, one.size());
		assertEquals(1, two.size());
		assertFalse(two.isEmpty());
		assertSame(REF_B, two.get(0));
	}

	@Test
	public void testAddToFrontOfList() {
		RefList<Ref> one = toList(REF_A);
		RefList<Ref> two = one.add(0, REF_B);
		assertNotSame(one, two);

		// one is not modified, but two is
		assertEquals(1, one.size());
		assertSame(REF_A, one.get(0));
		assertEquals(2, two.size());
		assertSame(REF_B, two.get(0));
		assertSame(REF_A, two.get(1));
	}

	@Test
	public void testAddToEndOfList() {
		RefList<Ref> one = toList(REF_A);
		RefList<Ref> two = one.add(1, REF_B);
		assertNotSame(one, two);

		// one is not modified, but two is
		assertEquals(1, one.size());
		assertSame(REF_A, one.get(0));
		assertEquals(2, two.size());
		assertSame(REF_A, two.get(0));
		assertSame(REF_B, two.get(1));
	}

	@Test
	public void testAddToMiddleOfListByInsertionPosition() {
		RefList<Ref> one = toList(REF_A, REF_c);

		assertEquals(-2, one.find(REF_B.getName()));

		RefList<Ref> two = one.add(one.find(REF_B.getName()), REF_B);
		assertNotSame(one, two);

		// one is not modified, but two is
		assertEquals(2, one.size());
		assertSame(REF_A, one.get(0));
		assertSame(REF_c, one.get(1));

		assertEquals(3, two.size());
		assertSame(REF_A, two.get(0));
		assertSame(REF_B, two.get(1));
		assertSame(REF_c, two.get(2));
	}

	@Test
	public void testPutNewEntry() {
		RefList<Ref> one = toList(REF_A, REF_c);
		RefList<Ref> two = one.put(REF_B);
		assertNotSame(one, two);

		// one is not modified, but two is
		assertEquals(2, one.size());
		assertSame(REF_A, one.get(0));
		assertSame(REF_c, one.get(1));

		assertEquals(3, two.size());
		assertSame(REF_A, two.get(0));
		assertSame(REF_B, two.get(1));
		assertSame(REF_c, two.get(2));
	}

	@Test
	public void testPutReplaceEntry() {
		Ref otherc = newRef(REF_c.getName());
		assertNotSame(REF_c, otherc);

		RefList<Ref> one = toList(REF_A, REF_c);
		RefList<Ref> two = one.put(otherc);
		assertNotSame(one, two);

		// one is not modified, but two is
		assertEquals(2, one.size());
		assertSame(REF_A, one.get(0));
		assertSame(REF_c, one.get(1));

		assertEquals(2, two.size());
		assertSame(REF_A, two.get(0));
		assertSame(otherc, two.get(1));
	}

	@Test
	public void testRemoveFrontOfList() {
		RefList<Ref> one = toList(REF_A, REF_B, REF_c);
		RefList<Ref> two = one.remove(0);
		assertNotSame(one, two);

		assertEquals(3, one.size());
		assertSame(REF_A, one.get(0));
		assertSame(REF_B, one.get(1));
		assertSame(REF_c, one.get(2));

		assertEquals(2, two.size());
		assertSame(REF_B, two.get(0));
		assertSame(REF_c, two.get(1));
	}

	@Test
	public void testRemoveMiddleOfList() {
		RefList<Ref> one = toList(REF_A, REF_B, REF_c);
		RefList<Ref> two = one.remove(1);
		assertNotSame(one, two);

		assertEquals(3, one.size());
		assertSame(REF_A, one.get(0));
		assertSame(REF_B, one.get(1));
		assertSame(REF_c, one.get(2));

		assertEquals(2, two.size());
		assertSame(REF_A, two.get(0));
		assertSame(REF_c, two.get(1));
	}

	@Test
	public void testRemoveEndOfList() {
		RefList<Ref> one = toList(REF_A, REF_B, REF_c);
		RefList<Ref> two = one.remove(2);
		assertNotSame(one, two);

		assertEquals(3, one.size());
		assertSame(REF_A, one.get(0));
		assertSame(REF_B, one.get(1));
		assertSame(REF_c, one.get(2));

		assertEquals(2, two.size());
		assertSame(REF_A, two.get(0));
		assertSame(REF_B, two.get(1));
	}

	@Test
	public void testRemoveMakesEmpty() {
		RefList<Ref> one = toList(REF_A);
		RefList<Ref> two = one.remove(1);
		assertNotSame(one, two);
		assertSame(two, RefList.emptyList());
	}

	@Test
	public void testToString() {
		StringBuilder exp = new StringBuilder();
		exp.append("[");
		exp.append(REF_A);
		exp.append(", ");
		exp.append(REF_B);
		exp.append("]");

		RefList<Ref> list = toList(REF_A, REF_B);
		assertEquals(exp.toString(), list.toString());
	}

	@Test
	public void testBuilder_ToString() {
		StringBuilder exp = new StringBuilder();
		exp.append("[");
		exp.append(REF_A);
		exp.append(", ");
		exp.append(REF_B);
		exp.append("]");

		RefList.Builder<Ref> list = new RefList.Builder<>();
		list.add(REF_A);
		list.add(REF_B);
		assertEquals(exp.toString(), list.toString());
	}

	@Test
	public void testFindContainsGet() {
		RefList<Ref> list = toList(REF_A, REF_B, REF_c);

		assertEquals(0, list.find("A"));
		assertEquals(1, list.find("B"));
		assertEquals(2, list.find("c"));

		assertEquals(-1, list.find("0"));
		assertEquals(-2, list.find("AB"));
		assertEquals(-3, list.find("a"));
		assertEquals(-4, list.find("z"));

		assertSame(REF_A, list.get("A"));
		assertSame(REF_B, list.get("B"));
		assertSame(REF_c, list.get("c"));
		assertNull(list.get("AB"));
		assertNull(list.get("z"));

		assertTrue(list.contains("A"));
		assertTrue(list.contains("B"));
		assertTrue(list.contains("c"));
		assertFalse(list.contains("AB"));
		assertFalse(list.contains("z"));
	}

	@Test
	public void testIterable() {
		RefList<Ref> list = toList(REF_A, REF_B, REF_c);

		int idx = 0;
		for (Ref ref : list)
			assertSame(list.get(idx++), ref);
		assertEquals(3, idx);

		Iterator<Ref> i = RefList.emptyList().iterator();
		try {
			i.next();
			fail("did not throw NoSuchElementException");
		} catch (NoSuchElementException err) {
			// expected
		}

		i = list.iterator();
		assertTrue(i.hasNext());
		assertSame(REF_A, i.next());
		try {
			i.remove();
			fail("did not throw UnsupportedOperationException");
		} catch (UnsupportedOperationException err) {
			// expected
		}
	}

	@Test
	public void testCopyLeadingPrefix() {
		RefList<Ref> one = toList(REF_A, REF_B, REF_c);
		RefList<Ref> two = one.copy(2).toRefList();
		assertNotSame(one, two);

		assertEquals(3, one.size());
		assertSame(REF_A, one.get(0));
		assertSame(REF_B, one.get(1));
		assertSame(REF_c, one.get(2));

		assertEquals(2, two.size());
		assertSame(REF_A, two.get(0));
		assertSame(REF_B, two.get(1));
	}

	@Test
	public void testCopyConstructorReusesArray() {
		RefList.Builder<Ref> one = new RefList.Builder<>();
		one.add(REF_A);

		RefList<Ref> two = new RefList<>(one.toRefList());
		one.set(0, REF_B);
		assertSame(REF_B, two.get(0));
	}

	private static RefList<Ref> toList(Ref... refs) {
		RefList.Builder<Ref> b = new RefList.Builder<>(refs.length);
		b.addAll(refs, 0, refs.length);
		return b.toRefList();
	}

	private static Ref newRef(String name) {
		return new ObjectIdRef.Unpeeled(Ref.Storage.LOOSE, name, ID);
	}
}
