/*
 * Copyright (C) 2017, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.diff;

import org.eclipse.jgit.errors.BinaryBlobException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectLoader;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class RawTextLoadTest extends RepositoryTestCase {
	private static byte[] generate(int size, int nullAt) {
		byte[] data = new byte[size];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) ((i % 72 == 0) ? '\n' : (i%10) + '0');
		}
		if (nullAt >= 0) {
			data[nullAt] = '\0';
		}
		return data;
	}

	private RawText textFor(byte[] data, int limit) throws IOException, BinaryBlobException {
		FileRepository repo = createBareRepository();
		ObjectId id;
		try (ObjectInserter ins = repo.getObjectDatabase().newInserter()) {
			id = ins.insert(Constants.OBJ_BLOB, data);
		}
		ObjectLoader ldr = repo.open(id);
		return RawText.load(ldr, limit);
	}

	@Test
	public void testSmallOK() throws Exception {
		byte[] data = generate(1000, -1);
		RawText result = textFor(data, 1 << 20);
		assertArrayEquals(result.content, data);
	}

	@Test
	public void testSmallNull() {
		byte[] data = generate(1000, 22);
		Exception thrown = assertThrows(
				Exception.class,
				() -> textFor(data, 1 << 20),
				"Expected doThing() to throw, but it didn't"
		);

		assertTrue(thrown.getMessage().contains("Expected doThing() to throw, but it didn't"));
	}

	@Test
	public void testBigOK() throws Exception {
		byte[] data = generate(10000, -1);
		RawText result = textFor(data, 1 << 20);
		assertArrayEquals(result.content, data);
	}

	@Test
	public void testBigWithNullAtStart()  {
		byte[] data = generate(10000, 22);
		BinaryBlobException thrown = assertThrows(
				BinaryBlobException.class,
				() -> textFor(data, 1 << 20)
		);
        assertNotNull(thrown);
	}

	@Test
	public void testBinaryThreshold() {
		byte[] data = generate(2 << 20, -1);
		BinaryBlobException thrown = assertThrows(
				BinaryBlobException.class,
				() -> textFor(data, 1 << 20)
		);
		assertNotNull(thrown);
	}
}
