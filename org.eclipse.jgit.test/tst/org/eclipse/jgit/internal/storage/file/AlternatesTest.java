/*
 * Copyright (C) 2017, Matthias Sohn <matthias.sohn@sap.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.internal.storage.file;

import static org.eclipse.jgit.lib.Constants.INFO_ALTERNATES;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.AbortedByHookException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.junit.JGitTestUtil;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.test.resources.SampleDataRepositoryTestCase;
import org.junit.jupiter.api.Test;

public class AlternatesTest extends SampleDataRepositoryTestCase {

	private FileRepository db2;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		db2 = createWorkRepository();
	}

	private void setAlternate(FileRepository from, FileRepository to)
			throws IOException {
		File alt = new File(from.getObjectDatabase().getDirectory(),
				INFO_ALTERNATES);
		alt.getParentFile().mkdirs();
		File fromDir = from.getObjectDatabase().getDirectory();
		File toDir = to.getObjectDatabase().getDirectory();
		Path relative = fromDir.toPath().relativize(toDir.toPath());
		write(alt, relative.toString() + "\n");
	}

	@Test
	public void testAlternate() throws Exception {
		setAlternate(db2, db);
		RevCommit c = createCommit();
		assertCommit(c);
		assertAlternateObjects(db2);
	}

	@Test
	public void testAlternateCyclic2() throws Exception {
		setAlternate(db2, db);
		setAlternate(db, db2);
		RevCommit c = createCommit();
		assertCommit(c);
		assertAlternateObjects(db2);
	}

	@Test
	public void testAlternateCyclic3() throws Exception {
		FileRepository db3 = createBareRepository();
		setAlternate(db2, db3);
		setAlternate(db3, db);
		setAlternate(db, db2);
		RevCommit c = createCommit();
		assertCommit(c);
		assertAlternateObjects(db2);
	}

	private RevCommit createCommit() throws IOException, GitAPIException,
			NoFilepatternException, NoHeadException, NoMessageException,
			UnmergedPathsException, ConcurrentRefUpdateException,
			WrongRepositoryStateException, AbortedByHookException {
		JGitTestUtil.writeTrashFile(db, "test", "test");
		Git git = Git.wrap(db2);
		git.add().addFilepattern("test").call();
		RevCommit c = git.commit().setMessage("adding test").call();
		return c;
	}

	private void assertCommit(RevCommit c) {
		ObjectDirectory od = db2.getObjectDatabase();
		assertTrue("can't find expected commit" + c.name(),
				od.has(c.toObjectId()));
	}

	private void assertAlternateObjects(FileRepository repo) {
		// check some objects in alternate
		final ObjectId alternateObjects[] = new ObjectId[] {
				ObjectId.fromString("49322bb17d3acc9146f98c97d078513228bbf3c0"),
				ObjectId.fromString("d0114ab8ac326bab30e3a657a0397578c5a1af88"),
				ObjectId.fromString("f73b95671f326616d66b2afb3bdfcdbbce110b44"),
				ObjectId.fromString("6020a3b8d5d636e549ccbd0c53e2764684bb3125"),
				ObjectId.fromString("0a3d7772488b6b106fb62813c4d6d627918d9181"),
				ObjectId.fromString("da0f8ed91a8f2f0f067b3bdf26265d5ca48cf82c"),
				ObjectId.fromString(
						"cd4bcfc27da62c6b840de700be1c60a7e69952a5") };
		ObjectDirectory od = repo.getObjectDatabase();
		for (ObjectId o : alternateObjects) {
			assertTrue(String.format("can't find object %s in alternate",
					o.getName()), od.has(o));
		}
	}
}
