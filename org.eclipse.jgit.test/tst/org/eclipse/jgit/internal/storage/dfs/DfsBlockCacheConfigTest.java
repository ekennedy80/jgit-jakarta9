/*
 * Copyright (C) 2016, Philipp Marx <philippmarx@gmx.de> and
 * other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v1.0 which accompanies this
 * distribution, is reproduced below, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.eclipse.jgit.internal.storage.dfs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

import org.eclipse.jgit.internal.JGitText;
import org.junit.jupiter.api.Test;

public class DfsBlockCacheConfigTest {

	@Test
	public void blockSizeNotPowerOfTwoExpectsException() {
		assertThrows(JGitText.get().blockSizeNotPowerOf2,
				IllegalArgumentException.class,
				() -> new DfsBlockCacheConfig().setBlockSize(1000));
	}

	@Test
	@SuppressWarnings("boxing")
	public void negativeBlockSizeIsConvertedToDefault() {
		DfsBlockCacheConfig config = new DfsBlockCacheConfig();
		config.setBlockSize(-1);

		assertThat(config.getBlockSize(), is(512));
	}

	@Test
	@SuppressWarnings("boxing")
	public void tooSmallBlockSizeIsConvertedToDefault() {
		DfsBlockCacheConfig config = new DfsBlockCacheConfig();
		config.setBlockSize(10);

		assertThat(config.getBlockSize(), is(512));
	}

	@Test
	@SuppressWarnings("boxing")
	public void validBlockSize() {
		DfsBlockCacheConfig config = new DfsBlockCacheConfig();
		config.setBlockSize(65536);

		assertThat(config.getBlockSize(), is(65536));
	}
}
