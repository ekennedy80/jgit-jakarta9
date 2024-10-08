/*
 * Copyright (C) 2010, Robin Stocker <robin@nibor.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Ref.Storage;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.SymbolicRef;
import org.eclipse.jgit.test.resources.SampleDataRepositoryTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test construction of merge message by {@link MergeMessageFormatter}.
 */
public class MergeMessageFormatterTest extends SampleDataRepositoryTestCase {

	private MergeMessageFormatter formatter;

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();

		RefUpdate createRemoteRefA = db
				.updateRef("refs/remotes/origin/remote-a");
		createRemoteRefA.setNewObjectId(db.resolve("refs/heads/a"));
		createRemoteRefA.update();

		RefUpdate createRemoteRefB = db
				.updateRef("refs/remotes/origin/remote-b");
		createRemoteRefB.setNewObjectId(db.resolve("refs/heads/b"));
		createRemoteRefB.update();

		formatter = new MergeMessageFormatter();
	}

	@Test
	public void testOneBranch() throws IOException {
		Ref a = db.exactRef("refs/heads/a");
		Ref master = db.exactRef("refs/heads/master");
		String message = formatter.format(Arrays.asList(a), master);
		assertEquals("Merge branch 'a'", message);
	}

	@Test
	public void testTwoBranches() throws IOException {
		Ref a = db.exactRef("refs/heads/a");
		Ref b = db.exactRef("refs/heads/b");
		Ref master = db.exactRef("refs/heads/master");
		String message = formatter.format(Arrays.asList(a, b), master);
		assertEquals("Merge branches 'a' and 'b'", message);
	}

	@Test
	public void testThreeBranches() throws IOException {
		Ref c = db.exactRef("refs/heads/c");
		Ref b = db.exactRef("refs/heads/b");
		Ref a = db.exactRef("refs/heads/a");
		Ref master = db.exactRef("refs/heads/master");
		String message = formatter.format(Arrays.asList(c, b, a), master);
		assertEquals("Merge branches 'c', 'b' and 'a'", message);
	}

	@Test
	public void testRemoteBranch() throws Exception {
		Ref remoteA = db.exactRef("refs/remotes/origin/remote-a");
		Ref master = db.exactRef("refs/heads/master");
		String message = formatter.format(Arrays.asList(remoteA), master);
		assertEquals("Merge remote-tracking branch 'origin/remote-a'", message);
	}

	@Test
	public void testMixed() throws IOException {
		Ref c = db.exactRef("refs/heads/c");
		Ref remoteA = db.exactRef("refs/remotes/origin/remote-a");
		Ref master = db.exactRef("refs/heads/master");
		String message = formatter.format(Arrays.asList(c, remoteA), master);
		assertEquals("Merge branch 'c', remote-tracking branch 'origin/remote-a'",
				message);
	}

	@Test
	public void testTag() throws IOException {
		Ref tagA = db.exactRef("refs/tags/A");
		Ref master = db.exactRef("refs/heads/master");
		String message = formatter.format(Arrays.asList(tagA), master);
		assertEquals("Merge tag 'A'", message);
	}

	@Test
	public void testCommit() throws IOException {
		ObjectId objectId = ObjectId
				.fromString("6db9c2ebf75590eef973081736730a9ea169a0c4");
		Ref commit = new ObjectIdRef.Unpeeled(Storage.LOOSE,
				objectId.getName(), objectId);
		Ref master = db.exactRef("refs/heads/master");
		String message = formatter.format(Arrays.asList(commit), master);
		assertEquals("Merge commit '6db9c2ebf75590eef973081736730a9ea169a0c4'",
				message);
	}

	@Test
	public void testPullWithUri() throws IOException {
		String name = "branch 'test' of http://egit.eclipse.org/jgit.git";
		ObjectId objectId = ObjectId
				.fromString("6db9c2ebf75590eef973081736730a9ea169a0c4");
		Ref remoteBranch = new ObjectIdRef.Unpeeled(Storage.LOOSE, name,
				objectId);
		Ref master = db.exactRef("refs/heads/master");
		String message = formatter.format(Arrays.asList(remoteBranch), master);
		assertEquals("Merge branch 'test' of http://egit.eclipse.org/jgit.git",
				message);
	}

	@Test
	public void testIntoOtherThanMaster() throws IOException {
		Ref a = db.exactRef("refs/heads/a");
		Ref b = db.exactRef("refs/heads/b");
		String message = formatter.format(Arrays.asList(a), b);
		assertEquals("Merge branch 'a' into b", message);
	}

	@Test
	public void testIntoHeadOtherThanMaster() throws IOException {
		Ref a = db.exactRef("refs/heads/a");
		Ref b = db.exactRef("refs/heads/b");
		SymbolicRef head = new SymbolicRef("HEAD", b);
		String message = formatter.format(Arrays.asList(a), head);
		assertEquals("Merge branch 'a' into b", message);
	}

	@Test
	public void testIntoSymbolicRefHeadPointingToMaster() throws IOException {
		Ref a = db.exactRef("refs/heads/a");
		Ref master = db.exactRef("refs/heads/master");
		SymbolicRef head = new SymbolicRef("HEAD", master);
		String message = formatter.format(Arrays.asList(a), head);
		assertEquals("Merge branch 'a'", message);
	}

	@Test
	public void testFormatWithConflictsNoFooter() {
		String originalMessage = "Header Line\n\nCommit body\n";
		String message = formatter.formatWithConflicts(originalMessage,
				List.of("path1"), '#');
		assertEquals("Header Line\n\nCommit body\n\n# Conflicts:\n#\tpath1\n",
				message);
	}

	@Test
	public void testFormatWithConflictsNoFooterNoLineBreak() {
		String originalMessage = "Header Line\n\nCommit body";
		String message = formatter.formatWithConflicts(originalMessage,
				List.of("path1"), '#');
		assertEquals("Header Line\n\nCommit body\n\n# Conflicts:\n#\tpath1\n",
				message);
	}

	@Test
	public void testFormatWithConflictsCustomCharacter() {
		String originalMessage = "Header Line\n\nCommit body";
		String message = formatter.formatWithConflicts(originalMessage,
				List.of("path1"), ';');
		assertEquals("Header Line\n\nCommit body\n\n; Conflicts:\n;\tpath1\n",
				message);
	}

	@Test
	public void testFormatWithConflictsWithFooters() {
		String originalMessage = "Header Line\n\nCommit body\n\nChangeId:"
				+ " I123456789123456789123456789123456789\nBug:1234567\n";
		String message = formatter.formatWithConflicts(originalMessage,
				List.of("path1"), '#');
		assertEquals(
				"Header Line\n\nCommit body\n\n# Conflicts:\n#\tpath1\n\n"
						+ "ChangeId: I123456789123456789123456789123456789\nBug:1234567\n",
				message);
	}

	@Test
	public void testFormatWithConflictsWithFooterlikeLineInBody() {
		String originalMessage = "Header Line\n\nCommit body\nBug:1234567\nMore Body\n\nChangeId:"
				+ " I123456789123456789123456789123456789\nBug:1234567\n";
		String message = formatter.formatWithConflicts(originalMessage,
				List.of("path1"), '#');
		assertEquals(
				"Header Line\n\nCommit body\nBug:1234567\nMore Body\n\n# Conflicts:\n#\tpath1\n\n"
						+ "ChangeId: I123456789123456789123456789123456789\nBug:1234567\n",
				message);
	}
}
