/*
 * Copyright (C) 2019, Matthias Sohn <matthias.sohn@sap.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.benchmarks;

import org.eclipse.jgit.internal.storage.file.FileSnapshot;
import org.eclipse.jgit.util.FileUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
public class CreateFileSnapshotBenchmark {

	Path path;

	Path testDir;

	@Setup
	public void setupBenchmark() throws IOException {
		testDir = Files.createTempDirectory("dir");
		path = testDir.resolve("toSnapshot");
		Files.createFile(path);
	}

	@TearDown
	public void teardown() throws IOException {
		FileUtils.delete(testDir.toFile(), FileUtils.RECURSIVE | FileUtils.RETRY);
	}

	@Benchmark
	@BenchmarkMode({ Mode.AverageTime })
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
	public Path testCreateFile() throws IOException {
		return Files.createTempFile(testDir, "create", "");
	}

	@Benchmark
	@BenchmarkMode({ Mode.AverageTime })
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	public FileSnapshot testCreateFileSnapshot() {
		return FileSnapshot.save(path.toFile());
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(CreateFileSnapshotBenchmark.class.getSimpleName())
				.forks(1).jvmArgs("-ea").build();
		new Runner(opt).run();
	}
}
