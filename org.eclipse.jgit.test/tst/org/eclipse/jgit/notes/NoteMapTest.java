/*
 * Copyright (C) 2010, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.notes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.MutableObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevBlob;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.RawParseUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.AfterEach;

public class NoteMapTest extends RepositoryTestCase {
	private TestRepository<Repository> tr;

	private ObjectReader reader;

	private ObjectInserter inserter;

	@Override
	@BeforeEach
	public void setUp(TestInfo testInfo) throws Exception {
		super.setUp(testInfo);

		tr = new TestRepository<>(db);
		reader = db.newObjectReader();
		inserter = db.newObjectInserter();
	}

	@Override
	@AfterEach
	public void tearDown() throws Exception {
		reader.close();
		inserter.close();
		super.tearDown();
	}

	@Test
	public void testReadFlatTwoNotes() throws Exception {
		RevBlob a = tr.blob("a");
		RevBlob b = tr.blob("b");
		RevBlob data1 = tr.blob("data1");
		RevBlob data2 = tr.blob("data2");

		RevCommit r = tr.commit() //
				.add(a.name(), data1) //
				.add(b.name(), data2) //
				.create();
		tr.parseBody(r);

		NoteMap map = NoteMap.read(reader, r);
		assertNotNull(map);

		assertTrue(map.contains(a));
		assertTrue(map.contains(b));
		assertEquals(data1, map.get(a));
		assertEquals(data2, map.get(b));

		assertFalse(map.contains(data1));
		Assertions.assertNull(map.get(data1));
	}

	@Test
	public void testReadFanout2_38() throws Exception {
		RevBlob a = tr.blob("a");
		RevBlob b = tr.blob("b");
		RevBlob data1 = tr.blob("data1");
		RevBlob data2 = tr.blob("data2");

		RevCommit r = tr.commit() //
				.add(fanout(2, a.name()), data1) //
				.add(fanout(2, b.name()), data2) //
				.create();
		tr.parseBody(r);

		NoteMap map = NoteMap.read(reader, r);
		assertNotNull(map);

		assertTrue(map.contains(a));
		assertTrue(map.contains(b));
		assertEquals(data1, map.get(a));
		assertEquals(data2, map.get(b));

		assertFalse(map.contains(data1));
		assertNull("no note for data1", map.get(data1));
	}

	@Test
	public void testReadFanout2_2_36() throws Exception {
		RevBlob a = tr.blob("a");
		RevBlob b = tr.blob("b");
		RevBlob data1 = tr.blob("data1");
		RevBlob data2 = tr.blob("data2");

		RevCommit r = tr.commit() //
				.add(fanout(4, a.name()), data1) //
				.add(fanout(4, b.name()), data2) //
				.create();
		tr.parseBody(r);

		NoteMap map = NoteMap.read(reader, r);
		assertNotNull(map);

		assertTrue(map.contains(a));
		assertTrue(map.contains(b));
		assertEquals(data1, map.get(a));
		assertEquals(data2, map.get(b));

		assertFalse(map.contains(data1));
		assertNull("no note for data1", map.get(data1));
	}

	@Test
	public void testReadFullyFannedOut() throws Exception {
		RevBlob a = tr.blob("a");
		RevBlob b = tr.blob("b");
		RevBlob data1 = tr.blob("data1");
		RevBlob data2 = tr.blob("data2");

		RevCommit r = tr.commit() //
				.add(fanout(38, a.name()), data1) //
				.add(fanout(38, b.name()), data2) //
				.create();
		tr.parseBody(r);

		NoteMap map = NoteMap.read(reader, r);
		assertNotNull(map);

		assertTrue(map.contains(a));
		assertTrue(map.contains(b));
		assertEquals(data1, map.get(a));
		assertEquals(data2, map.get(b));

		assertFalse(map.contains(data1));
		assertNull("no note for data1", map.get(data1));
	}

	@Test
	public void testGetCachedBytes() throws Exception {
		final String exp = "this is test data";
		RevBlob a = tr.blob("a");
		RevBlob data = tr.blob(exp);

		RevCommit r = tr.commit() //
				.add(a.name(), data) //
				.create();
		tr.parseBody(r);

		NoteMap map = NoteMap.read(reader, r);
		byte[] act = map.getCachedBytes(a, exp.length() * 4);
		assertNotNull(act);
		assertEquals(exp, RawParseUtils.decode(act));
	}

	@Test
	public void testWriteUnchangedFlat() throws Exception {
		RevBlob a = tr.blob("a");
		RevBlob b = tr.blob("b");
		RevBlob data1 = tr.blob("data1");
		RevBlob data2 = tr.blob("data2");

		RevCommit r = tr.commit() //
				.add(a.name(), data1) //
				.add(b.name(), data2) //
				.add(".gitignore", "") //
				.add("zoo-animals.txt", "") //
				.create();
		tr.parseBody(r);

		NoteMap map = NoteMap.read(reader, r);
		assertTrue(map.contains(a));
		assertTrue(map.contains(b));

		RevCommit n = commitNoteMap(map);
		Assertions.assertNotSame(r, n, "is new commit");
		Assertions.assertSame(r.getTree(), n.getTree(), "same tree");
	}

	@Test
	public void testWriteUnchangedFanout2_38() throws Exception {
		RevBlob a = tr.blob("a");
		RevBlob b = tr.blob("b");
		RevBlob data1 = tr.blob("data1");
		RevBlob data2 = tr.blob("data2");

		RevCommit r = tr.commit() //
				.add(fanout(2, a.name()), data1) //
				.add(fanout(2, b.name()), data2) //
				.add(".gitignore", "") //
				.add("zoo-animals.txt", "") //
				.create();
		tr.parseBody(r);

		NoteMap map = NoteMap.read(reader, r);
		assertTrue(map.contains(a));
		assertTrue(map.contains(b));

		// This is a non-lazy map, so we'll be looking at the leaf buckets.
		RevCommit n = commitNoteMap(map);
		Assertions.assertNotSame(r, n, "is new commit");
		Assertions.assertSame(r.getTree(), n.getTree(), "same tree");

		// Use a lazy-map for the next round of the same test.
		map = NoteMap.read(reader, r);
		n = commitNoteMap(map);
		Assertions.assertNotSame(r, n, "is new commit");
		Assertions.assertSame(r.getTree(), n.getTree(), "same tree");
	}

	@Test
	public void testCreateFromEmpty() throws Exception {
		RevBlob a = tr.blob("a");
		RevBlob b = tr.blob("b");
		RevBlob data1 = tr.blob("data1");
		RevBlob data2 = tr.blob("data2");

		NoteMap map = NoteMap.newEmptyMap();
		assertFalse(map.contains(a));
		assertFalse(map.contains(b));

		map.set(a, data1);
		map.set(b, data2);

		assertEquals(data1, map.get(a));
		assertEquals(data2, map.get(b));

		map.remove(a);
		map.remove(b);

		assertFalse(map.contains(b));

		map.set(a, "data1", inserter);
		assertEquals(data1, map.get(a));

		map.set(a, null, inserter);
		assertFalse(map.contains(a));
	}

	@Test
	public void testEditFlat() throws Exception {
		RevBlob a = tr.blob("a");
		RevBlob b = tr.blob("b");
		RevBlob data1 = tr.blob("data1");
		RevBlob data2 = tr.blob("data2");

		RevCommit r = tr.commit() //
				.add(a.name(), data1) //
				.add(b.name(), data2) //
				.add(".gitignore", "") //
				.add("zoo-animals.txt", b) //
				.create();
		tr.parseBody(r);

		NoteMap map = NoteMap.read(reader, r);
		map.set(a, data2);
		map.set(b, null);
		map.set(data1, b);
		map.set(data2, null);

		assertEquals(data2, map.get(a));
		assertEquals(b, map.get(data1));
		assertFalse(map.contains(b));
		assertFalse(map.contains(data2));

		MutableObjectId id = new MutableObjectId();
		for (int p = 42; p > 0; p--) {
			id.setByte(1, p);
			map.set(id, data1);
		}

		for (int p = 42; p > 0; p--) {
			id.setByte(1, p);
			assertTrue(map.contains(id));
		}

		RevCommit n = commitNoteMap(map);
		map = NoteMap.read(reader, n);
		assertEquals(data2, map.get(a));
		assertEquals(b, map.get(data1));
		assertFalse(map.contains(b));
		assertFalse(map.contains(data2));
		assertEquals(b, TreeWalk
				.forPath(reader, "zoo-animals.txt", n.getTree()).getObjectId(0));
	}

	@Test
	public void testEditFanout2_38() throws Exception {
		RevBlob a = tr.blob("a");
		RevBlob b = tr.blob("b");
		RevBlob data1 = tr.blob("data1");
		RevBlob data2 = tr.blob("data2");

		RevCommit r = tr.commit() //
				.add(fanout(2, a.name()), data1) //
				.add(fanout(2, b.name()), data2) //
				.add(".gitignore", "") //
				.add("zoo-animals.txt", b) //
				.create();
		tr.parseBody(r);

		NoteMap map = NoteMap.read(reader, r);
		map.set(a, data2);
		map.set(b, null);
		map.set(data1, b);
		map.set(data2, null);

		assertEquals(data2, map.get(a));
		assertEquals(b, map.get(data1));
		assertFalse(map.contains(b));
		assertFalse(map.contains(data2));
		RevCommit n = commitNoteMap(map);

		map.set(a, null);
		map.set(data1, null);
		assertFalse(map.contains(a));
		assertFalse(map.contains(data1));

		map = NoteMap.read(reader, n);
		assertEquals(data2, map.get(a));
		assertEquals(b, map.get(data1));
		assertFalse(map.contains(b));
		assertFalse(map.contains(data2));
		assertEquals(b, TreeWalk
				.forPath(reader, "zoo-animals.txt", n.getTree()).getObjectId(0));
	}

	@Test
	public void testLeafSplitsWhenFull() throws Exception {
		RevBlob data1 = tr.blob("data1");
		MutableObjectId idBuf = new MutableObjectId();

		RevCommit r = tr.commit() //
				.add(data1.name(), data1) //
				.create();
		tr.parseBody(r);

		NoteMap map = NoteMap.read(reader, r);
		for (int i = 0; i < 254; i++) {
			idBuf.setByte(Constants.OBJECT_ID_LENGTH - 1, i);
			map.set(idBuf, data1);
		}

		RevCommit n = commitNoteMap(map);
		try (TreeWalk tw = new TreeWalk(reader)) {
			tw.reset(n.getTree());
			while (tw.next()) {
				assertFalse(tw.isSubtree());
			}
		}

		for (int i = 254; i < 256; i++) {
			idBuf.setByte(Constants.OBJECT_ID_LENGTH - 1, i);
			map.set(idBuf, data1);
		}
		idBuf.setByte(Constants.OBJECT_ID_LENGTH - 2, 1);
		map.set(idBuf, data1);
		n = commitNoteMap(map);

		// The 00 bucket is fully split.
		String path = fanout(38, idBuf.name());
		try (TreeWalk tw = TreeWalk.forPath(reader, path, n.getTree())) {
			assertNotNull(tw);
		}

		// The other bucket is not.
		path = fanout(2, data1.name());
		try (TreeWalk tw = TreeWalk.forPath(reader, path, n.getTree())) {
			assertNotNull(tw);
		}
	}

	@Test
	public void testRemoveDeletesTreeFanout2_38() throws Exception {
		RevBlob a = tr.blob("a");
		RevBlob data1 = tr.blob("data1");
		RevTree empty = tr.tree();

		RevCommit r = tr.commit() //
				.add(fanout(2, a.name()), data1) //
				.create();
		tr.parseBody(r);

		NoteMap map = NoteMap.read(reader, r);
		map.set(a, null);

		RevCommit n = commitNoteMap(map);
		assertEquals(empty, n.getTree());
	}

	@Test
	public void testIteratorEmptyMap() {
		Iterator<Note> it = NoteMap.newEmptyMap().iterator();
		assertFalse(it.hasNext());
	}

	@Test
	public void testIteratorFlatTree() throws Exception {
		RevBlob a = tr.blob("a");
		RevBlob b = tr.blob("b");
		RevBlob data1 = tr.blob("data1");
		RevBlob data2 = tr.blob("data2");
		RevBlob nonNote = tr.blob("non note");

		RevCommit r = tr.commit() //
				.add(a.name(), data1) //
				.add(b.name(), data2) //
				.add("nonNote", nonNote) //
				.create();
		tr.parseBody(r);

		Iterator it = NoteMap.read(reader, r).iterator();
		assertEquals(2, count(it));
	}

	@Test
	public void testIteratorFanoutTree2_38() throws Exception {
		RevBlob a = tr.blob("a");
		RevBlob b = tr.blob("b");
		RevBlob data1 = tr.blob("data1");
		RevBlob data2 = tr.blob("data2");
		RevBlob nonNote = tr.blob("non note");

		RevCommit r = tr.commit() //
				.add(fanout(2, a.name()), data1) //
				.add(fanout(2, b.name()), data2) //
				.add("nonNote", nonNote) //
				.create();
		tr.parseBody(r);

		Iterator it = NoteMap.read(reader, r).iterator();
		assertEquals(2, count(it));
	}

	@Test
	public void testIteratorFanoutTree2_2_36() throws Exception {
		RevBlob a = tr.blob("a");
		RevBlob b = tr.blob("b");
		RevBlob data1 = tr.blob("data1");
		RevBlob data2 = tr.blob("data2");
		RevBlob nonNote = tr.blob("non note");

		RevCommit r = tr.commit() //
				.add(fanout(4, a.name()), data1) //
				.add(fanout(4, b.name()), data2) //
				.add("nonNote", nonNote) //
				.create();
		tr.parseBody(r);

		Iterator it = NoteMap.read(reader, r).iterator();
		assertEquals(2, count(it));
	}

	@Test
	public void testIteratorFullyFannedOut() throws Exception {
		RevBlob a = tr.blob("a");
		RevBlob b = tr.blob("b");
		RevBlob data1 = tr.blob("data1");
		RevBlob data2 = tr.blob("data2");
		RevBlob nonNote = tr.blob("non note");

		RevCommit r = tr.commit() //
				.add(fanout(38, a.name()), data1) //
				.add(fanout(38, b.name()), data2) //
				.add("nonNote", nonNote) //
				.create();
		tr.parseBody(r);

		Iterator it = NoteMap.read(reader, r).iterator();
		assertEquals(2, count(it));
	}

	@Test
	public void testShorteningNoteRefName() throws Exception {
		String expectedShortName = "review";
		String noteRefName = Constants.R_NOTES + expectedShortName;
		assertEquals(expectedShortName, NoteMap.shortenRefName(noteRefName));
		String nonNoteRefName = Constants.R_HEADS + expectedShortName;
		assertEquals(nonNoteRefName, NoteMap.shortenRefName(nonNoteRefName));
	}

	private RevCommit commitNoteMap(NoteMap map) throws IOException {
		tr.tick(600);

		CommitBuilder builder = new CommitBuilder();
		builder.setTreeId(map.writeTree(inserter));
		tr.setAuthorAndCommitter(builder);
		return tr.getRevWalk().parseCommit(inserter.insert(builder));
	}

	private static String fanout(int prefix, String name) {
		StringBuilder r = new StringBuilder();
		int i = 0;
		for (; i < prefix && i < name.length(); i += 2) {
			if (i != 0)
				r.append('/');
			r.append(name.charAt(i + 0));
			r.append(name.charAt(i + 1));
		}
		if (i < name.length()) {
			if (i != 0)
				r.append('/');
			r.append(name.substring(i));
		}
		return r.toString();
	}

	private static int count(Iterator it) {
		int c = 0;
		while (it.hasNext()) {
			c++;
			it.next();
		}
		return c;
	}
}
