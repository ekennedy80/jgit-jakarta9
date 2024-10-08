/*
 * Copyright (c) 2020, Google LLC  and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.internal.storage.dfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.eclipse.jgit.api.GarbageCollectCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.TransportBundleStream;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DfsBundleWriterTest {
	private TestRepository<InMemoryRepository> git;

	private InMemoryRepository repo;

	@BeforeEach
	public void setUp() throws IOException {
		DfsRepositoryDescription desc = new DfsRepositoryDescription("test");
		git = new TestRepository<>(new InMemoryRepository(desc));
		repo = git.getRepository();
	}

	@Test
	public void makeBundle_containsUnreferencedObject() throws Exception {
		RevCommit commit0 = git.commit().message("0").create();
		RevCommit commit1 = git.commit().message("1").parent(commit0).create();
		git.update("master", commit1);

		RevCommit commit2 = git.commit().message("0").create();

		byte[] bundle = makeBundle();
		try (Repository newRepo = new InMemoryRepository(
				new DfsRepositoryDescription("copy"))) {
			fetchFromBundle(newRepo, bundle);
			Ref ref = newRepo.exactRef("refs/heads/master");
			assertNotNull(ref);
			assertEquals(commit1.toObjectId(), ref.getObjectId());

			// Unreferenced objects are included as well.
			assertTrue(newRepo.getObjectDatabase().has(commit2));
		}
	}

	@Test
	public void makeBundle_containsObjectInGcRestPack() throws Exception {
		RevCommit commit0 = git.commit().message("0").create();
		RevCommit commit1 = git.commit().message("1").parent(commit0).create();
		git.update("master", commit1);

		RevCommit commit2 = git.commit().message("0").create();

		// This moves unreachable commit2 to GC_REST pack.
		GarbageCollectCommand gc = Git.wrap(repo).gc();
		gc.call();

		byte[] bundle = makeBundle();
		try (Repository newRepo = new InMemoryRepository(
				new DfsRepositoryDescription("copy"))) {
			fetchFromBundle(newRepo, bundle);
			Ref ref = newRepo.exactRef("refs/heads/master");
			assertNotNull(ref);
			assertEquals(commit1.toObjectId(), ref.getObjectId());

			// Unreferenced objects in GC_REST pack are included as well.
			assertTrue(newRepo.getObjectDatabase().has(commit2));
		}
	}

	private byte[] makeBundle() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DfsBundleWriter.writeEntireRepositoryAsBundle(
				NullProgressMonitor.INSTANCE, out, repo);
		return out.toByteArray();
	}

	private static FetchResult fetchFromBundle(Repository newRepo,
			byte[] bundle) throws Exception {
		URIish uri = new URIish("in-memory://");
		ByteArrayInputStream in = new ByteArrayInputStream(bundle);
		RefSpec rs = new RefSpec("refs/heads/*:refs/heads/*");
		Set<RefSpec> refs = Collections.singleton(rs);
		try (TransportBundleStream transport = new TransportBundleStream(
				newRepo, uri, in)) {
			return transport.fetch(NullProgressMonitor.INSTANCE, refs);
		}
	}
}
