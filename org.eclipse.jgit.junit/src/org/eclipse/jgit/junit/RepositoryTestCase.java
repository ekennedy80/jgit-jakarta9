/*
 * Copyright (C) 2009, Google Inc.
 * Copyright (C) 2007-2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006-2007, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2009, Yann Simon <yann.simon.fr@gmail.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.junit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheCheckout;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.io.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Base class for most JGit unit tests.
 * Sets up a predefined test repository and has support for creating additional
 * repositories and destroying them when the tests are finished.
 */
public abstract class 	RepositoryTestCase extends LocalDiskRepositoryTestCase {
	/**
	 * Copy a file
	 *
	 * @param src
	 *            file to copy
	 * @param dst
	 *            destination of the copy
	 * @throws IOException
	 *             if an IO error occurred
	 */
	protected static void copyFile(File src, File dst)
			throws IOException {
		try (FileInputStream fis = new FileInputStream(src);
				FileOutputStream fos = new FileOutputStream(dst)) {
			final byte[] buf = new byte[4096];
			int r;
			while ((r = fis.read(buf)) > 0) {
				fos.write(buf, 0, r);
			}
		}
	}

	/**
	 * Write a trash file
	 *
	 * @param name
	 *            file name
	 * @param data
	 *            file content
	 * @return the trash file
	 * @throws IOException
	 *             if an IO error occurred
	 */
	protected File writeTrashFile(String name, String data)
			throws IOException {
		return JGitTestUtil.writeTrashFile(db, name, data);
	}

	/**
	 * Create a symbolic link
	 *
	 * @param link
	 *            the path of the symbolic link to create
	 * @param target
	 *            the target of the symbolic link
	 * @return the path to the symbolic link
	 * @throws Exception
	 *             if an error occurred
	 * @since 4.2
	 */
	protected Path writeLink(String link, String target)
			throws Exception {
		return JGitTestUtil.writeLink(db, link, target);
	}

	/**
	 * Write a trash file
	 *
	 * @param subdir
	 *            in working tree
	 * @param name
	 *            file name
	 * @param data
	 *            file content
	 * @return the trash file
	 * @throws IOException
	 *             if an IO error occurred
	 */
	protected File writeTrashFile(final String subdir, final String name,
			final String data)
			throws IOException {
		return JGitTestUtil.writeTrashFile(db, subdir, name, data);
	}

	/**
	 * Read content of a file
	 *
	 * @param name
	 *            file name
	 * @return the file's content
	 * @throws IOException
	 *             if an IO error occurred
	 */
	protected String read(String name) throws IOException {
		return JGitTestUtil.read(db, name);
	}

	/**
	 * Check if file exists
	 *
	 * @param name
	 *            file name
	 * @return if the file exists
	 */
	protected boolean check(String name) {
		return JGitTestUtil.check(db, name);
	}

	/**
	 * Delete a trash file
	 *
	 * @param name
	 *            file name
	 * @throws IOException
	 *             if an IO error occurred
	 */
	protected void deleteTrashFile(String name) throws IOException {
		JGitTestUtil.deleteTrashFile(db, name);
	}

	/**
	 * Check content of a file.
	 *
	 * @param f
	 *            file
	 * @param checkData
	 *            expected content
	 * @throws IOException
	 *             if an IO error occurred
	 */
	protected static void checkFile(File f, String checkData)
			throws IOException {
		try (Reader r = new InputStreamReader(new FileInputStream(f),
				UTF_8)) {
			if (!checkData.isEmpty()) {
				char[] data = new char[checkData.length()];
				assertEquals(data.length, r.read(data));
				assertEquals(checkData, new String(data));
			}
			assertEquals(-1, r.read());
		}
	}

	/** Test repository, initialized for this test case. */
	protected FileRepository db;

	/** Working directory of {@link #db}. */
	protected File trash;

	/**
	 * Happens before each test
	 * @throws Exception an excpetion
	 */
	@BeforeEach
	public void setUp(TestInfo testInfo) throws Exception {
		super.setUp(testInfo);
		db = createWorkRepository();
		trash = db.getWorkTree();
	}

	/**
	 * Happens before each test
	 * @throws Exception an exception
	 */
	@AfterEach
	public void tearDown() throws Exception {
		db.close();
		super.tearDown();
	}

	/**
	 * Represent the state of the index in one String. This representation is
	 * useful when writing tests which do assertions on the state of the index.
	 * By default information about path, mode, stage (if different from 0) is
	 * included. A bitmask controls which additional info about
	 * modificationTimes, smudge state and length is included.
	 * <p>
	 * The format of the returned string is described with this BNF:
	 *
	 * <pre>
	 * result = ( "[" path mode stage? time? smudge? length? sha1? content? "]" )* .
	 * mode = ", mode:" number .
	 * stage = ", stage:" number .
	 * time = ", time:t" timestamp-index .
	 * smudge = "" | ", smudged" .
	 * length = ", length:" number .
	 * sha1 = ", sha1:" hex-sha1 .
	 * content = ", content:" blob-data .
	 * </pre>
	 *
	 * 'stage' is only presented when the stage is different from 0. All
	 * reported time stamps are mapped to strings like "t0", "t1", ... "tn". The
	 * smallest reported time-stamp will be called "t0". This allows to write
	 * assertions against the string although the concrete value of the time
	 * stamps is unknown.
	 *
	 * @param includedOptions
	 *            a bitmask constructed out of the constants {@link #MOD_TIME},
	 *            {@link #SMUDGE}, {@link #LENGTH}, {@link #CONTENT_ID} and
	 *            {@link #CONTENT} controlling which info is present in the
	 *            resulting string.
	 * @return a string encoding the index state
	 * @throws IOException
	 *             if an IO error occurred
	 */
	public String indexState(int includedOptions)
			throws IOException {
		return indexState(db, includedOptions);
	}

	/**
	 * Resets the index to represent exactly some filesystem content. E.g. the
	 * following call will replace the index with the working tree content:
	 * <p>
	 * <code>resetIndex(new FileSystemIterator(db))</code>
	 * <p>
	 * This method can be used by testcases which first prepare a new commit
	 * somewhere in the filesystem (e.g. in the working-tree) and then want to
	 * have an index which matches their prepared content.
	 *
	 * @param treeItr
	 *            a {@link org.eclipse.jgit.treewalk.FileTreeIterator} which
	 *            determines which files should go into the new index
	 * @throws FileNotFoundException
	 *             file was not found
	 * @throws IOException
	 *             if an IO error occurred
	 */
	protected void resetIndex(FileTreeIterator treeItr)
			throws IOException {
		try (ObjectInserter inserter = db.newObjectInserter()) {
			DirCacheBuilder builder = db.lockDirCache().builder();
			DirCacheEntry dce;

			while (!treeItr.eof()) {
				long len = treeItr.getEntryLength();

				dce = new DirCacheEntry(treeItr.getEntryPathString());
				dce.setFileMode(treeItr.getEntryFileMode());
				dce.setLastModified(treeItr.getEntryLastModifiedInstant());
				dce.setLength((int) len);
				try (FileInputStream in = new FileInputStream(
						treeItr.getEntryFile())) {
					dce.setObjectId(
							inserter.insert(Constants.OBJ_BLOB, len, in));
				}
				builder.add(dce);
				treeItr.next(1);
			}
			builder.commit();
			inserter.flush();
		}
	}

	/**
	 * Replaces '\' by '/'
	 *
	 * @param str
	 *            the string in which backslashes should be replaced
	 * @return the resulting string with slashes
	 * @since 4.2
	 */
	public static String slashify(String str) {
		str = str.replace('\\', '/');
		return str;
	}

	/**
	 * Waits until it is guaranteed that a subsequent file modification has a
	 * younger modification timestamp than the modification timestamp of the
	 * given file. This is done by touching a temporary file, reading the
	 * lastmodified attribute and, if needed, sleeping. After sleeping this loop
	 * starts again until the filesystem timer has advanced enough. The
	 * temporary file will be created as a sibling of lastFile.
	 *
	 * @param lastFile
	 *            the file on which we want to wait until the filesystem timer
	 *            has advanced more than the lastmodification timestamp of this
	 *            file
	 * @return return the last measured value of the filesystem timer which is
	 *         greater than then the lastmodification time of lastfile.
	 * @throws InterruptedException
	 *             if thread was interrupted
	 * @throws IOException
	 *             if an IO error occurred
	 * @since 5.1.9
	 */
	public static Instant fsTick(File lastFile)
			throws InterruptedException,
			IOException {
		File tmp;
		FS fs = FS.DETECTED;
		if (lastFile == null) {
			lastFile = tmp = File
					.createTempFile("fsTickTmpFile", null);
		} else {
			if (!fs.exists(lastFile)) {
				throw new FileNotFoundException(lastFile.getPath());
			}
			tmp = File.createTempFile("fsTickTmpFile", null,
					lastFile.getParentFile());
		}
		long res = FS.getFileStoreAttributes(tmp.toPath())
				.getFsTimestampResolution().toNanos();
		long sleepTime = res / 10;
		try {
			Instant startTime = fs.lastModifiedInstant(lastFile);
			Instant actTime = fs.lastModifiedInstant(tmp);
			while (actTime.compareTo(startTime) <= 0) {
				TimeUnit.NANOSECONDS.sleep(sleepTime);
				FileUtils.touch(tmp.toPath());
				actTime = fs.lastModifiedInstant(tmp);
			}
			return actTime;
		} finally {
			FileUtils.delete(tmp);
		}
	}

	/**
	 * Create a branch
	 *
	 * @param objectId
	 *            new value to create the branch on
	 * @param branchName
	 *            branch name
	 * @throws IOException
	 *             if an IO error occurred
	 */
	protected void createBranch(ObjectId objectId, String branchName)
			throws IOException {
		RefUpdate updateRef = db.updateRef(branchName);
		updateRef.setNewObjectId(objectId);
		updateRef.update();
	}

	/**
	 * Get all Refs
	 *
	 * @return list of refs
	 * @throws IOException
	 *             if an IO error occurred
	 */
	public List<Ref> getRefs() throws IOException {
		return db.getRefDatabase().getRefs();
	}

	/**
	 * Checkout a branch
	 *
	 * @param branchName
	 *            branch name
	 * @throws IOException
	 *             if an IO error occurred
	 */
	protected void checkoutBranch(String branchName)
			throws IOException {
		try (RevWalk walk = new RevWalk(db)) {
			RevCommit head = walk.parseCommit(db.resolve(Constants.HEAD));
			RevCommit branch = walk.parseCommit(db.resolve(branchName));
			DirCacheCheckout dco = new DirCacheCheckout(db,
					head.getTree().getId(), db.lockDirCache(),
					branch.getTree().getId());
			dco.setFailOnConflict(true);
			dco.checkout();
		}
		// update the HEAD
		RefUpdate refUpdate = db.updateRef(Constants.HEAD);
		refUpdate.setRefLogMessage("checkout: moving to " + branchName, false);
		refUpdate.link(branchName);
	}

	/**
	 * Writes a number of files in the working tree. The first content specified
	 * will be written into a file named '0', the second into a file named "1"
	 * and so on. If <code>null</code> is specified as content then this file is
	 * skipped.
	 *
	 * @param contents the contents which should be written into the files
	 * @return the File object associated to the last written file.
	 * @throws IOException          if an IO error occurred
	 * @throws InterruptedException if thread was interrupted
	 */
	protected File writeTrashFiles(String... contents)
			throws IOException, InterruptedException {
				File f = null;
				for (int i = 0; i < contents.length; i++)
					if (contents[i] != null) {
                        f = writeTrashFile(Integer.toString(i), contents[i]);
					}
				return f;
			}

	/**
	 * Commit a file with the specified contents on the specified branch,
	 * creating the branch if it didn't exist before.
	 * <p>
	 * It switches back to the original branch after the commit if there was
	 * one.
	 *
	 * @param filename
	 *            file name
	 * @param contents
	 *            file content
	 * @param branch
	 *            branch name
	 * @return the created commit
	 */
	protected RevCommit commitFile(String filename, String contents, String branch) {
		try (Git git = new Git(db)) {
			Repository repo = git.getRepository();
			String originalBranch = repo.getFullBranch();
			boolean empty = repo.resolve(Constants.HEAD) == null;
			if (!empty) {
				if (repo.findRef(branch) == null)
					git.branchCreate().setName(branch).call();
				git.checkout().setName(branch).call();
			}

			writeTrashFile(filename, contents);
			git.add().addFilepattern(filename).call();
			RevCommit commit = git.commit()
					.setMessage(branch + ": " + filename).call();

			if (originalBranch != null)
				git.checkout().setName(originalBranch).call();
			else if (empty)
				git.branchCreate().setName(branch).setStartPoint(commit).call();

			return commit;
		} catch (IOException | GitAPIException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create <code>DirCacheEntry</code>
	 *
	 * @param path
	 *            file path
	 * @param mode
	 *            file mode
	 * @return the DirCacheEntry
	 */
	protected DirCacheEntry createEntry(String path, FileMode mode) {
		return createEntry(path, mode, DirCacheEntry.STAGE_0, path);
	}

	/**
	 * Create <code>DirCacheEntry</code>
	 *
	 * @param path
	 *            file path
	 * @param mode
	 *            file mode
	 * @param content
	 *            file content
	 * @return the DirCacheEntry
	 */
	protected DirCacheEntry createEntry(final String path, final FileMode mode,
			final String content) {
		return createEntry(path, mode, DirCacheEntry.STAGE_0, content);
	}

	/**
	 * Create <code>DirCacheEntry</code>
	 *
	 * @param path
	 *            file path
	 * @param mode
	 *            file mode
	 * @param stage
	 *            stage index of the new entry
	 * @param content
	 *            file content
	 * @return the DirCacheEntry
	 */
	protected DirCacheEntry createEntry(final String path, final FileMode mode,
			final int stage, final String content) {
		final DirCacheEntry entry = new DirCacheEntry(path, stage);
		entry.setFileMode(mode);
		try (ObjectInserter.Formatter formatter = new ObjectInserter.Formatter()) {
			entry.setObjectId(formatter.idFor(
					Constants.OBJ_BLOB, Constants.encode(content)));
		}
		return entry;
	}

	/**
	 * Create <code>DirCacheEntry</code>
	 *
	 * @param path
	 *            file path
	 * @param objectId
	 *            of the entry
	 * @return the DirCacheEntry
	 */
	protected DirCacheEntry createGitLink(String path, AnyObjectId objectId) {
		final DirCacheEntry entry = new DirCacheEntry(path,
				DirCacheEntry.STAGE_0);
		entry.setFileMode(FileMode.GITLINK);
		entry.setObjectId(objectId);
		return entry;
	}

	/**
	 * Assert files are equal
	 *
	 * @param expected
	 *            expected file
	 * @param actual
	 *            actual file
	 * @throws IOException
	 *             if an IO error occurred
	 */
	public static void assertEqualsFile(File expected, File actual)
			throws IOException {
		assertEquals(expected.getCanonicalFile(), actual.getCanonicalFile());
	}
}
