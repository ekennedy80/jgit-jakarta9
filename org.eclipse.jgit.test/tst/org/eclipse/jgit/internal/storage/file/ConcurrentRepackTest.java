/*
 * Copyright (C) 2009-2010, Google Inc.
 * Copyright (C) 2009, Robin Rosenberg <robin.rosenberg@dewire.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.storage.file;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.storage.pack.PackExt;
import org.eclipse.jgit.internal.storage.pack.PackWriter;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

public class ConcurrentRepackTest extends RepositoryTestCase {
	@Override
	@BeforeEach
	public void setUp() throws Exception {
		WindowCacheConfig windowCacheConfig = new WindowCacheConfig();
		windowCacheConfig.setPackedGitOpenFiles(1);
		windowCacheConfig.install();
		super.setUp();
	}

	@AfterEach
    @Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		new WindowCacheConfig().install();
	}

	@Test
	public void testObjectInNewPack() throws IncorrectObjectTypeException,
			IOException {
		// Create a new object in a new pack, and test that it is present.
		//
		final Repository eden = createBareRepository();
		final RevObject o1 = writeBlob(eden, "o1");
		pack(eden, o1);
		assertEquals(o1.name(), parse(o1).name());
	}

	@Test
	public void testObjectMovedToNewPack1()
			throws IncorrectObjectTypeException, IOException {
		// Create an object and pack it. Then remove that pack and put the
		// object into a different pack file, with some other object. We
		// still should be able to access the objects.
		//
		final Repository eden = createBareRepository();
		final RevObject o1 = writeBlob(eden, "o1");
		final File[] out1 = pack(eden, o1);
		assertEquals(o1.name(), parse(o1).name());

		final RevObject o2 = writeBlob(eden, "o2");
		pack(eden, o2, o1);

		// Force close, and then delete, the old pack.
		//
		whackCache();
		delete(out1);

		// Now here is the interesting thing. Will git figure the new
		// object exists in the new pack, and not the old one.
		//
		assertEquals(o2.name(), parse(o2).name());
		assertEquals(o1.name(), parse(o1).name());
	}

	@Test
	public void testObjectMovedWithinPack()
			throws IncorrectObjectTypeException, IOException {
		// Create an object and pack it.
		//
		final Repository eden = createBareRepository();
		final RevObject o1 = writeBlob(eden, "o1");
		final File[] out1 = pack(eden, o1);
		assertEquals(o1.name(), parse(o1).name());

		// Force close the old pack.
		//
		whackCache();

		// Now overwrite the old pack in place. This method of creating a
		// different pack under the same file name is partially broken. We
		// should also have a different file name because the list of objects
		// within the pack has been modified.
		//
		final RevObject o2 = writeBlob(eden, "o2");
		try (PackWriter pw = new PackWriter(eden)) {
			pw.addObject(o2);
			pw.addObject(o1);
			write(out1, pw);
		}

		// Try the old name, then the new name. The old name should cause the
		// pack to reload when it opens and the index and pack mismatch.
		//
		assertEquals(o1.name(), parse(o1).name());
		assertEquals(o2.name(), parse(o2).name());
	}

	@Test
	public void testObjectMovedToNewPack2()
			throws IncorrectObjectTypeException, IOException {
		// Create an object and pack it. Then remove that pack and put the
		// object into a different pack file, with some other object. We
		// still should be able to access the objects.
		//
		final Repository eden = createBareRepository();
		final RevObject o1 = writeBlob(eden, "o1");
		final File[] out1 = pack(eden, o1);
		assertEquals(o1.name(), parse(o1).name());

		final ObjectLoader load1 = db.open(o1, Constants.OBJ_BLOB);
		assertNotNull(load1);

		final RevObject o2 = writeBlob(eden, "o2");
		pack(eden, o2, o1);

		// Force close, and then delete, the old pack.
		//
		whackCache();
		delete(out1);

		// Now here is the interesting thing... can the loader we made
		// earlier still resolve the object, even though its underlying
		// pack is gone, but the object still exists.
		//
		final ObjectLoader load2 = db.open(o1, Constants.OBJ_BLOB);
		assertNotNull(load2);
		assertNotSame(load1, load2);

		final byte[] data2 = load2.getCachedBytes();
		final byte[] data1 = load1.getCachedBytes();
		assertNotNull(data2);
		assertNotNull(data1);
		assertNotSame(data1, data2); // cache should be per-pack, not per object
		assertArrayEquals(data1, data2);
		assertEquals(load2.getType(), load1.getType());
	}

	private static void whackCache() {
		final WindowCacheConfig config = new WindowCacheConfig();
		config.setPackedGitOpenFiles(1);
		config.install();
	}

	private RevObject parse(AnyObjectId id)
			throws MissingObjectException, IOException {
		try (RevWalk rw = new RevWalk(db)) {
			return rw.parseAny(id);
		}
	}

	private File[] pack(Repository src, RevObject... list)
			throws IOException {
		try (PackWriter pw = new PackWriter(src)) {
			for (RevObject o : list) {
				pw.addObject(o);
			}

			PackFile packFile = new PackFile(
					db.getObjectDatabase().getPackDirectory(), pw.computeName(),
					PackExt.PACK);
			PackFile idxFile = packFile.create(PackExt.INDEX);
			final File[] files = new File[] { packFile, idxFile };
			write(files, pw);
			return files;
		}
	}

	private static void write(File[] files, PackWriter pw)
			throws IOException {
		final Instant begin = FS.DETECTED
				.lastModifiedInstant(files[0].getParentFile());
		NullProgressMonitor m = NullProgressMonitor.INSTANCE;

		try (OutputStream out = new BufferedOutputStream(
				new FileOutputStream(files[0]))) {
			pw.writePack(m, m, out);
		}

		try (OutputStream out = new BufferedOutputStream(
				new FileOutputStream(files[1]))) {
			pw.writeIndex(out);
		}

		touch(begin, files[0].getParentFile());
	}

	private static void delete(File[] list) throws IOException {
		final Instant begin = FS.DETECTED
				.lastModifiedInstant(list[0].getParentFile());
		for (File f : list) {
			FileUtils.delete(f);
			assertFalse(f + " was removed", f.exists());
		}
		touch(begin, list[0].getParentFile());
	}

	private static void touch(Instant begin, File dir) throws IOException {
		while (begin.compareTo(FS.DETECTED.lastModifiedInstant(dir)) >= 0) {
			try {
				Thread.sleep(25);
			} catch (InterruptedException ie) {
				//
			}
			FS.DETECTED.setLastModified(dir.toPath(), Instant.now());
		}
	}

	private RevObject writeBlob(Repository repo, String data)
			throws IOException {
		final byte[] bytes = Constants.encode(data);
		final ObjectId id;
		try (ObjectInserter inserter = repo.newObjectInserter()) {
			id = inserter.insert(Constants.OBJ_BLOB, bytes);
			inserter.flush();
		}
		try {
			parse(id);
			fail("Object " + id.name() + " should not exist in test repository");
		} catch (MissingObjectException e) {
			// Ok
		}
		try (RevWalk revWalk = new RevWalk(repo)) {
			return revWalk.lookupBlob(id);
		}
	}
}
