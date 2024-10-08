/*
 * Copyright (C) 2012, 2014 IBM Corporation and others. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.pgm;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.CLIRepositoryTestCase;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.pgm.internal.CLIText;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MergeTest extends CLIRepositoryTestCase {

	private Git git;

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		git = new Git(db);
		git.commit().setMessage("initial commit").call();
	}

	@Test
	public void testMergeSelf() throws Exception {
		assertEquals("Already up-to-date.", execute("git merge master")[0]);
	}

	@Test
	public void testSquashSelf() throws Exception {
		assertEquals(" (nothing to squash)Already up-to-date.",
				execute("git merge master --squash")[0]);
	}

	@Test
	public void testFastForward() throws Exception {
		git.branchCreate().setName("side").call();
		writeTrashFile("file", "master");
		git.add().addFilepattern("file").call();
		git.commit().setMessage("commit").call();
		git.checkout().setName("side").call();

		assertArrayEquals(new String[] { "Updating 6fd41be..26a81a1",
				"Fast-forward", "" }, execute("git merge master"));
	}

	@Test
	public void testMerge() throws Exception {
		git.branchCreate().setName("side").call();
		writeTrashFile("master", "content");
		git.add().addFilepattern("master").call();
		git.commit().setMessage("master commit").call();
		git.checkout().setName("side").call();
		writeTrashFile("side", "content");
		git.add().addFilepattern("side").call();
		git.commit().setMessage("side commit").call();

		assertEquals("Merge made by the '" + MergeStrategy.RECURSIVE.getName()
				+ "' strategy.", execute("git merge master")[0]);
	}

	@Test
	public void testMergeNoCommit() throws Exception {
		git.branchCreate().setName("side").call();
		writeTrashFile("master", "content");
		git.add().addFilepattern("master").call();
		git.commit().setMessage("master commit").call();
		git.checkout().setName("side").call();
		writeTrashFile("side", "content");
		git.add().addFilepattern("side").call();
		git.commit().setMessage("side commit").call();

		assertEquals(
				"Automatic merge went well; stopped before committing as requested",
				execute("git merge --no-commit master")[0]);
	}

	@Test
	public void testMergeNoCommitSquash() throws Exception {
		git.branchCreate().setName("side").call();
		writeTrashFile("master", "content");
		git.add().addFilepattern("master").call();
		git.commit().setMessage("master commit").call();
		git.checkout().setName("side").call();
		writeTrashFile("side", "content");
		git.add().addFilepattern("side").call();
		git.commit().setMessage("side commit").call();

		assertArrayEquals(
				new String[] {
						"Squash commit -- not updating HEAD",
						"Automatic merge went well; stopped before committing as requested",
						"" }, execute("git merge --no-commit --squash master"));
	}

	@Test
	public void testSquash() throws Exception {
		git.branchCreate().setName("side").call();
		writeTrashFile("file1", "content1");
		git.add().addFilepattern("file1").call();
		git.commit().setMessage("file1 commit").call();
		writeTrashFile("file2", "content2");
		git.add().addFilepattern("file2").call();
		git.commit().setMessage("file2 commit").call();
		git.checkout().setName("side").call();
		writeTrashFile("side", "content");
		git.add().addFilepattern("side").call();
		git.commit().setMessage("side commit").call();

		assertArrayEquals(
				new String[] { "Squash commit -- not updating HEAD",
						"Automatic merge went well; stopped before committing as requested",
						"" },
				execute("git merge master --squash"));
	}

	@Test
	public void testNoFastForward() throws Exception {
		git.branchCreate().setName("side").call();
		writeTrashFile("file", "master");
		git.add().addFilepattern("file").call();
		git.commit().setMessage("commit").call();
		git.checkout().setName("side").call();

		assertEquals("Merge made by the 'recursive' strategy.",
				execute("git merge master --no-ff")[0]);
		assertArrayEquals(new String[] {
				"commit 6db23724012376e8407fc24b5da4277a9601be81", //
				"Author: GIT_COMMITTER_NAME <GIT_COMMITTER_EMAIL>", //
				"Date:   Sat Aug 15 20:12:58 2009 -0330", //
				"", //
				"    Merge branch 'master' into side", //
				"", //
				"commit 6fd41be26b7ee41584dd997f665deb92b6c4c004", //
				"Author: GIT_COMMITTER_NAME <GIT_COMMITTER_EMAIL>", //
				"Date:   Sat Aug 15 20:12:58 2009 -0330", //
				"", //
				"    initial commit", //
				"", //
				"commit 26a81a1c6a105551ba703a8b6afc23994cacbae1", //
				"Author: GIT_COMMITTER_NAME <GIT_COMMITTER_EMAIL>", //
				"Date:   Sat Aug 15 20:12:58 2009 -0330", //
				"", //
				"    commit", //
				"", //
				""
		}, execute("git log"));
	}

	@Test
	public void testNoFastForwardAndSquash() throws Exception {
		assertEquals(
				CLIText.fatalError(CLIText.get().cannotCombineSquashWithNoff),
				executeUnchecked("git merge master --no-ff --squash")[0]);
	}

	@Test
	public void testFastForwardOnly() throws Exception {
		git.branchCreate().setName("side").call();
		writeTrashFile("file", "master");
		git.add().addFilepattern("file").call();
		git.commit().setMessage("commit#1").call();
		git.checkout().setName("side").call();
		writeTrashFile("file", "side");
		git.add().addFilepattern("file").call();
		git.commit().setMessage("commit#2").call();

		assertEquals(CLIText.fatalError(CLIText.get().ffNotPossibleAborting),
				executeUnchecked("git merge master --ff-only")[0]);
	}

	@Test
	public void testMergeWithUserMessage() throws Exception {
		git.branchCreate().setName("side").call();
		writeTrashFile("master", "content");
		git.add().addFilepattern("master").call();
		git.commit().setMessage("master commit").call();
		git.checkout().setName("side").call();
		writeTrashFile("side", "content");
		git.add().addFilepattern("side").call();
		git.commit().setMessage("side commit").call();

		assertEquals("Merge made by the '" + MergeStrategy.RECURSIVE.getName()
				+ "' strategy.",
				execute("git merge master -m \"user message\"")[0]);

		Iterator<RevCommit> it = git.log().call().iterator();
		RevCommit newHead = it.next();
		assertEquals("user message", newHead.getFullMessage());
	}
}
