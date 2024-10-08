/*
 * Copyright (C) 2015, Google Inc.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.eclipse.jgit.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.Assert.fail;

import org.eclipse.jgit.errors.PackProtocolException;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;

/** Tests for receive-pack utilities. */
public class ReceivePackTest {
	@Test
	public void parseCommand() throws Exception {
		String o = "0000000000000000000000000000000000000000";
		String n = "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef";
		String r = "refs/heads/master";
		ReceiveCommand cmd = ReceivePack.parseCommand(o + " " + n + " " + r);
		assertEquals(ObjectId.zeroId(), cmd.getOldId());
		assertEquals("deadbeefdeadbeefdeadbeefdeadbeefdeadbeef",
				cmd.getNewId().name());
		assertEquals("refs/heads/master", cmd.getRefName());

		assertParseCommandFails(null);
		assertParseCommandFails("");
		assertParseCommandFails(o.substring(35) + " " + n.substring(35)
				+ " " + r + "\n");
		assertParseCommandFails(o + " " + n + " " + r + "\n");
		assertParseCommandFails(o + " " + n + " " + "refs^foo");
		assertParseCommandFails(o + " " + n.substring(10) + " " + r);
		assertParseCommandFails(o.substring(10) + " " + n + " " + r);
		assertParseCommandFails("X" + o.substring(1) + " " + n + " " + r);
		assertParseCommandFails(o + " " + "X" + n.substring(1) + " " + r);
	}

	private void assertParseCommandFails(String input) {
		try {
			ReceivePack.parseCommand(input);
			fail();
		} catch (PackProtocolException e) {
			// Expected.
		}
	}
}
