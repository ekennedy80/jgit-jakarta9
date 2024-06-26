/*
 * Copyright (C) 2011, Abhishek Bhatnagar <abhatnag@redhat.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.api;

import static org.eclipse.jgit.lib.Constants.DOT_GIT_MODULES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for CleanCommand
 */
public class CleanCommandTest extends RepositoryTestCase {
	private Git git;

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		git = new Git(db);

		// create test files
		writeTrashFile("File1.txt", "Hello world");
		writeTrashFile("File2.txt", "Delete Me");
		writeTrashFile("File3.txt", "Delete Me");

		// create files in sub-directories.
		writeTrashFile("sub-noclean/File1.txt", "Hello world");
		writeTrashFile("sub-noclean/File2.txt", "Delete Me");
		writeTrashFile("sub-clean/File4.txt", "Delete Me");
		writeTrashFile("sub-noclean/Ignored.txt", "Ignored");
		writeTrashFile(".gitignore", "/ignored-dir\n/sub-noclean/Ignored.txt");
		writeTrashFile("ignored-dir/Ignored2.txt", "Ignored");

		// add and commit first file
		git.add().addFilepattern("File1.txt").call();
		git.add().addFilepattern("sub-noclean/File1.txt").call();
		git.add().addFilepattern(".gitignore").call();
		git.commit().setMessage("Initial commit").call();
	}

	@Test
	public void testClean() throws NoWorkTreeException, GitAPIException {
		// create status
		StatusCommand command = git.status();
		Status status = command.call();
		Set<String> files = status.getUntracked();
		assertFalse(files.isEmpty());

		// run clean
		Set<String> cleanedFiles = git.clean().call();

		status = git.status().call();
		files = status.getUntracked();

		assertTrue(files.size() == 1); // one remains (directories not cleaned)
		assertTrue(cleanedFiles.contains("File2.txt"));
		assertTrue(cleanedFiles.contains("File3.txt"));
		assertTrue(!cleanedFiles.contains("sub-noclean/File1.txt"));
		assertTrue(cleanedFiles.contains("sub-noclean/File2.txt"));
		assertTrue(!cleanedFiles.contains("sub-clean/File4.txt"));
	}

	@Test
	public void testCleanDirs() throws NoWorkTreeException, GitAPIException {
		// create status
		StatusCommand command = git.status();
		Status status = command.call();
		Set<String> files = status.getUntracked();
		assertFalse(files.isEmpty());

		// run clean
		Set<String> cleanedFiles = git.clean().setCleanDirectories(true).call();

		status = git.status().call();
		files = status.getUntracked();

		assertTrue(files.isEmpty());
		assertTrue(cleanedFiles.contains("File2.txt"));
		assertTrue(cleanedFiles.contains("File3.txt"));
		assertTrue(!cleanedFiles.contains("sub-noclean/File1.txt"));
		assertTrue(cleanedFiles.contains("sub-noclean/File2.txt"));
		assertTrue(cleanedFiles.contains("sub-clean/"));
	}

	@Test
	public void testCleanWithPaths() throws NoWorkTreeException,
			GitAPIException {
		// create status
		StatusCommand command = git.status();
		Status status = command.call();
		Set<String> files = status.getUntracked();
		assertFalse(files.isEmpty());

		// run clean with setPaths
		Set<String> paths = new TreeSet<>();
		paths.add("File3.txt");
		Set<String> cleanedFiles = git.clean().setPaths(paths).call();

		status = git.status().call();
		files = status.getUntracked();
		assertTrue(files.size() == 3);
		assertTrue(cleanedFiles.contains("File3.txt"));
		assertFalse(cleanedFiles.contains("File2.txt"));
	}

	@Test
	public void testCleanWithDryRun() throws NoWorkTreeException,
			GitAPIException {
		// create status
		StatusCommand command = git.status();
		Status status = command.call();
		Set<String> files = status.getUntracked();
		assertFalse(files.isEmpty());

		// run clean
		Set<String> cleanedFiles = git.clean().setDryRun(true).call();

		status = git.status().call();
		files = status.getUntracked();

		assertEquals(4, files.size());
		assertTrue(cleanedFiles.contains("File2.txt"));
		assertTrue(cleanedFiles.contains("File3.txt"));
		assertTrue(!cleanedFiles.contains("sub-noclean/File1.txt"));
		assertTrue(cleanedFiles.contains("sub-noclean/File2.txt"));
	}

	@Test
	public void testCleanDirsWithDryRun() throws NoWorkTreeException,
			GitAPIException {
		// create status
		StatusCommand command = git.status();
		Status status = command.call();
		Set<String> files = status.getUntracked();
		assertFalse(files.isEmpty());

		// run clean
		Set<String> cleanedFiles = git.clean().setDryRun(true)
				.setCleanDirectories(true).call();

		status = git.status().call();
		files = status.getUntracked();

		assertTrue(files.size() == 4);
		assertTrue(cleanedFiles.contains("File2.txt"));
		assertTrue(cleanedFiles.contains("File3.txt"));
		assertTrue(!cleanedFiles.contains("sub-noclean/File1.txt"));
		assertTrue(cleanedFiles.contains("sub-noclean/File2.txt"));
		assertTrue(cleanedFiles.contains("sub-clean/"));
	}

	@Test
	public void testCleanWithDryRunAndNoIgnore() throws NoWorkTreeException,
			GitAPIException {
		// run clean
		Set<String> cleanedFiles = git.clean().setDryRun(true).setIgnore(false)
				.call();

		Status status = git.status().call();
		Set<String> files = status.getIgnoredNotInIndex();

		assertTrue(files.size() == 2);
		assertTrue(cleanedFiles.contains("sub-noclean/Ignored.txt"));
		assertTrue(!cleanedFiles.contains("ignored-dir/"));
	}

	@Test
	public void testCleanDirsWithDryRunAndNoIgnore()
			throws NoWorkTreeException, GitAPIException {
		// run clean
		Set<String> cleanedFiles = git.clean().setDryRun(true).setIgnore(false)
				.setCleanDirectories(true).call();

		Status status = git.status().call();
		Set<String> files = status.getIgnoredNotInIndex();

		assertTrue(files.size() == 2);
		assertTrue(cleanedFiles.contains("sub-noclean/Ignored.txt"));
		assertTrue(cleanedFiles.contains("ignored-dir/"));
	}

	@Test
	public void testCleanDirsWithPrefixFolder() throws Exception {
		String path = "sub/foo.txt";
		writeTrashFile(path, "sub is a prefix of sub-noclean");
		git.add().addFilepattern(path).call();
		Status beforeCleanStatus = git.status().call();
		assertTrue(beforeCleanStatus.getAdded().contains(path));

		Set<String> cleanedFiles = git.clean().setCleanDirectories(true).call();

		// The "sub" directory should not be cleaned.
		assertTrue(!cleanedFiles.contains(path + "/"));

		assertTrue(cleanedFiles.contains("File2.txt"));
		assertTrue(cleanedFiles.contains("File3.txt"));
		assertTrue(!cleanedFiles.contains("sub-noclean/File1.txt"));
		assertTrue(cleanedFiles.contains("sub-noclean/File2.txt"));
		assertTrue(cleanedFiles.contains("sub-clean/"));
		assertTrue(cleanedFiles.size() == 4);
	}

	@Test
	public void testCleanDirsWithSubmodule() throws Exception {
		SubmoduleAddCommand command = new SubmoduleAddCommand(db);
		String path = "sub";
		command.setPath(path);
		String uri = db.getDirectory().toURI().toString();
		command.setURI(uri);
		try (Repository repo = command.call()) {
			// Unused
		}

		Status beforeCleanStatus = git.status().call();
		assertTrue(beforeCleanStatus.getAdded().contains(DOT_GIT_MODULES));
		assertTrue(beforeCleanStatus.getAdded().contains(path));

		Set<String> cleanedFiles = git.clean().setCleanDirectories(true).call();

		// The submodule should not be cleaned.
		assertTrue(!cleanedFiles.contains(path + "/"));

		assertTrue(cleanedFiles.contains("File2.txt"));
		assertTrue(cleanedFiles.contains("File3.txt"));
		assertTrue(!cleanedFiles.contains("sub-noclean/File1.txt"));
		assertTrue(cleanedFiles.contains("sub-noclean/File2.txt"));
		assertTrue(cleanedFiles.contains("sub-clean/"));
		assertTrue(cleanedFiles.size() == 4);
	}

	@Test
	public void testCleanDirsWithRepository() throws Exception {
		// Set up a repository inside the outer repository
		String innerRepoName = "inner-repo";
		File innerDir = new File(trash, innerRepoName);
		innerDir.mkdir();
		InitCommand initRepoCommand = new InitCommand();
		initRepoCommand.setDirectory(innerDir);
		initRepoCommand.call();

		Status beforeCleanStatus = git.status().call();
		Set<String> untrackedFolders = beforeCleanStatus.getUntrackedFolders();
		Set<String> untrackedFiles = beforeCleanStatus.getUntracked();

		// The inner repository should be listed as an untracked file
		assertTrue(untrackedFiles.contains(innerRepoName));

		// The inner repository should not be listed as an untracked folder
		assertTrue(!untrackedFolders.contains(innerRepoName));

		Set<String> cleanedFiles = git.clean().setCleanDirectories(true).call();

		// The inner repository should not be cleaned.
		assertTrue(!cleanedFiles.contains(innerRepoName + "/"));

		assertTrue(cleanedFiles.contains("File2.txt"));
		assertTrue(cleanedFiles.contains("File3.txt"));
		assertTrue(!cleanedFiles.contains("sub-noclean/File1.txt"));
		assertTrue(cleanedFiles.contains("sub-noclean/File2.txt"));
		assertTrue(cleanedFiles.contains("sub-clean/"));
		assertTrue(cleanedFiles.size() == 4);

		Set<String> forceCleanedFiles = git.clean().setCleanDirectories(true)
				.setForce(true).call();

		// The inner repository should be cleaned this time
		assertTrue(forceCleanedFiles.contains(innerRepoName + "/"));
	}

	@Test
	// To proof Bug 514434. No assertions, but before the bugfix
	// this test was throwing Exceptions
	public void testFilesShouldBeCleanedInSubSubFolders()
			throws IOException, NoFilepatternException, GitAPIException {
		writeTrashFile(".gitignore",
				"/ignored-dir\n/sub-noclean/Ignored.txt\n/this_is_ok\n/this_is/not_ok\n");
		git.add().addFilepattern(".gitignore").call();
		git.commit().setMessage("adding .gitignore").call();
		writeTrashFile("this_is_ok/more/subdirs/file.txt", "1");
		writeTrashFile("this_is/not_ok/more/subdirs/file.txt", "2");
		git.clean().setCleanDirectories(true).setIgnore(false).call();
	}

	@Test
	public void testPrefix() throws Exception {
		File a = writeTrashFile("a.txt", "a");
		File b = writeTrashFile("a/a.txt", "sub a");
		File dir = b.getParentFile();
		git.clean().call();
		assertFalse(a.exists());
		assertTrue(dir.exists());
		assertTrue(b.exists());
	}

	@Test
	public void testPrefixWithDir() throws Exception {
		File a = writeTrashFile("a.txt", "a");
		File b = writeTrashFile("a/a.txt", "sub a");
		File dir = b.getParentFile();
		git.clean().setCleanDirectories(true).call();
		assertFalse(a.exists());
		assertFalse(dir.exists());
	}
}
