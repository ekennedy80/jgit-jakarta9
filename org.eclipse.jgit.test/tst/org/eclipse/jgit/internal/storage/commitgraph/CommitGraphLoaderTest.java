/*
 * Copyright (C) 2022, Tencent.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.storage.commitgraph;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.jgit.internal.storage.commitgraph.CommitGraph.CommitData;
import org.eclipse.jgit.junit.JGitTestUtil;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;

/**
 * Test CommitGraphLoader by reading the commit-graph file generated by Cgit.
 */
public class CommitGraphLoaderTest {

	private CommitGraph commitGraph;

	@Test
	public void readCommitGraphV1() throws Exception {
		commitGraph = CommitGraphLoader
				.open(JGitTestUtil.getTestResourceFile("commit-graph.v1"));
		assertNotNull(commitGraph);
		assertEquals(10, commitGraph.getCommitCnt());
		verifyGraphObjectIndex();

		assertCommitData("85b0176af27fa1640868f061f224d01e0b295f59",
				new int[] { 5, 6 }, 1670570408L, 3, 0);
		assertCommitData("d4f7c00aab3f0160168c9e5991abb6194a4e0d9e",
				new int[] {}, 1670569901L, 1, 1);
		assertCommitData("4d03aaf9c20c97d6ccdc05cb7f146b1deb6c01d5",
				new int[] { 5 }, 1670570119L, 3, 2);
		assertCommitData("a2f409b753880bf83b18bfb433dd340a6185e8be",
				new int[] { 7 }, 1670569935L, 3, 3);
		assertCommitData("431343847343979bbe31127ed905a24fed9a636c",
				new int[] { 3, 2, 8 }, 1670570644L, 4, 4);
		assertCommitData("c3f745ad8928ef56b5dbf33740fc8ede6b598290",
				new int[] { 1 }, 1670570106L, 2, 5);
		assertCommitData("95b12422c8ea4371e54cd58925eeed9d960ff1f0",
				new int[] { 1 }, 1670570163L, 2, 6);
		assertCommitData("de0ea882503cdd9c984c0a43238014569a123cac",
				new int[] { 1 }, 1670569921L, 2, 7);
		assertCommitData("102c9d6481559b1a113eb66bf55085903de6fb00",
				new int[] { 6 }, 1670570616L, 3, 8);
		assertCommitData("b5de2a84867f8ffc6321649dabf8c0680661ec03",
				new int[] { 7, 5 }, 1670570364L, 3, 9);
	}

	private void verifyGraphObjectIndex() {
		for (int i = 0; i < commitGraph.getCommitCnt(); i++) {
			ObjectId id = commitGraph.getObjectId(i);
			int pos = commitGraph.findGraphPosition(id);
			assertEquals(i, pos);
		}
	}

	private void assertCommitData(String expectedTree, int[] expectedParents,
			long expectedCommitTime, int expectedGeneration, int graphPos) {
		CommitData commitData = commitGraph.getCommitData(graphPos);
		assertEquals(ObjectId.fromString(expectedTree), commitData.getTree());
		assertArrayEquals(expectedParents, commitData.getParents());
		assertEquals(expectedCommitTime, commitData.getCommitTime());
		assertEquals(expectedGeneration, commitData.getGeneration());
	}
}
