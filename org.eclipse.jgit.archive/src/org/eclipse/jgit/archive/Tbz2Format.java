/*
 * Copyright (C) 2013 Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.archive;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.eclipse.jgit.api.ArchiveCommand;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * bzip2-compressed tarball (tar.bz2) format.
 */
public final class Tbz2Format extends BaseFormat
		implements ArchiveCommand.Format<ArchiveOutputStream<TarArchiveEntry>> {
	private static final List<String> SUFFIXES = Collections
			.unmodifiableList(Arrays.asList(".tar.bz2", ".tbz", ".tbz2")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	private final ArchiveCommand.Format<ArchiveOutputStream<TarArchiveEntry>> tarFormat = new TarFormat();

	@Override
	public ArchiveOutputStream createArchiveOutputStream(OutputStream s)
			throws IOException {
		return createArchiveOutputStream(s,
				Collections.<String, Object> emptyMap());
	}

	@Override
	public ArchiveOutputStream createArchiveOutputStream(OutputStream s,
			Map<String, Object> o) throws IOException {
		BZip2CompressorOutputStream out;
		int compressionLevel = getCompressionLevel(o);
		if (compressionLevel != -1) {
			out = new BZip2CompressorOutputStream(s, compressionLevel);
		} else {
			out = new BZip2CompressorOutputStream(s);
		}
		return tarFormat.createArchiveOutputStream(out, o);
	}

	@Override
	public void putEntry(ArchiveOutputStream<TarArchiveEntry> out,
			ObjectId tree, String path, FileMode mode, ObjectLoader loader)
			throws IOException {
		tarFormat.putEntry(out, tree, path, mode, loader);
	}

	@Override
	public Iterable<String> suffixes() {
		return SUFFIXES;
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof Tbz2Format);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
