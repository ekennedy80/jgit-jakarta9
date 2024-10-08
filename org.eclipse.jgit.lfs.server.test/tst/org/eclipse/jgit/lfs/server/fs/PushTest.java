/*
 * Copyright (C) 2018, Markus Duft <markus.duft@ssi-schaefer.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.lfs.server.fs;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteAddCommand;
//import org.eclipse.jgit.junit.JGitTestUtil;
//import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lfs.BuiltinLFS;
import org.eclipse.jgit.lib.*;
//import org.eclipse.jgit.revwalk.RevCommit;
//import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
//import org.eclipse.jgit.treewalk.TreeWalk;
//import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.FileUtils;
//import org.eclipse.jgit.util.IO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

//import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

//import static java.nio.charset.StandardCharsets.UTF_8;
//import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PushTest extends LfsServerTest {

	Git git;

//	private TestRepository localDb;

	private Repository remoteDb;

	@Override
	@BeforeEach
	public void setup() throws Exception {
		super.setup();

		BuiltinLFS.register();

		Path rtmp = Files.createTempDirectory("jgit_test_");
		remoteDb = FileRepositoryBuilder.create(rtmp.toFile());
		remoteDb.create(true);

		Path tmp = Files.createTempDirectory("jgit_test_");
		Repository db = FileRepositoryBuilder
				.create(tmp.resolve(".git").toFile());
		db.create(false);
		StoredConfig cfg = db.getConfig();
		cfg.setString("filter", "lfs", "usejgitbuiltin", "true");
		cfg.setString("lfs", null, "url", server.getURI().toString() + "/lfs");
		cfg.save();

//		localDb = new TestRepository<>(db);
//		localDb.branch("master").commit().add(".gitattributes",
//				"*.bin filter=lfs diff=lfs merge=lfs -text ").create();
		git = Git.wrap(db);

		URIish uri = new URIish(
				"file://" + remoteDb.getDirectory());
		RemoteAddCommand radd = git.remoteAdd();
		radd.setUri(uri);
		radd.setName(Constants.DEFAULT_REMOTE_NAME);
		radd.call();

		git.checkout().setName("master").call();
		git.push().call();
	}

	@AfterEach
	public void cleanup() throws Exception {
		remoteDb.close();
//		localDb.getRepository().close();
//		FileUtils.delete(localDb.getRepository().getWorkTree(),
//				FileUtils.RECURSIVE);
		FileUtils.delete(remoteDb.getDirectory(), FileUtils.RECURSIVE);
	}

//	@Test
//	public void testPushSimple() throws Exception {
//		JGitTestUtil.writeTrashFile(localDb.getRepository(), "a.bin",
//				"1234567");
//		git.add().addFilepattern("a.bin").call();
//		RevCommit commit = git.commit().setMessage("add lfs blob").call();
//		git.push().call();
//
//		// check object in remote db, should be LFS pointer
//		ObjectId id = commit.getId();
//		try (RevWalk walk = new RevWalk(remoteDb)) {
//			RevCommit rc = walk.parseCommit(id);
//			try (TreeWalk tw = new TreeWalk(walk.getObjectReader())) {
//				tw.addTree(rc.getTree());
//				tw.setFilter(PathFilter.create("a.bin"));
//				tw.next();
//
//				assertEquals(tw.getPathString(), "a.bin");
//				ObjectLoader ldr = walk.getObjectReader()
//						.open(tw.getObjectId(0), Constants.OBJ_BLOB);
//				try(InputStream is = ldr.openStream()) {
//					assertEquals(
//							"version https://git-lfs.github.com/spec/v1\noid sha256:8bb0cf6eb9b17d0f7d22b456f121257dc1254e1f01665370476383ea776df414\nsize 7\n",
//							new String(IO
//									.readWholeStream(is,
//											(int) ldr.getSize())
//									.array(), UTF_8));
//				}
//			}
//
//		}
//
//		assertEquals(
//				"[POST /lfs/objects/batch 200, PUT /lfs/objects/8bb0cf6eb9b17d0f7d22b456f121257dc1254e1f01665370476383ea776df414 200]",
//				server.getRequests().toString());
//	}

	@Test
	public void testDeleteBranch() throws Exception {
		String branch = "new-branch";
		git.branchCreate().setName(branch).call();

		String destRef = Constants.R_HEADS + branch;
		git.push().setRefSpecs(new RefSpec().setSource(branch).setDestination(destRef)).call();

		// Should not fail on push.
		git.branchDelete().setBranchNames(branch).setForce(true).call();
		git.push().setRefSpecs(new RefSpec().setSource(null).setDestination(destRef)).call();

		assertTrue(server.getRequests().isEmpty());
	}
}
