/*
 * Copyright (C) 2010, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.transport;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.Deflater;

import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.UnpackException;
import org.eclipse.jgit.internal.storage.file.ObjectDirectory;
import org.eclipse.jgit.internal.storage.pack.BinaryDelta;
import org.eclipse.jgit.junit.LocalDiskRepositoryTestCase;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevBlob;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.NB;
import org.eclipse.jgit.util.TemporaryBuffer;
import org.junit.jupiter.api.*;

public class ReceivePackAdvertiseRefsHookTest extends LocalDiskRepositoryTestCase {
	private static final NullProgressMonitor PM = NullProgressMonitor.INSTANCE;

	private static final String R_MASTER = Constants.R_HEADS + Constants.MASTER;

	private static final String R_PRIVATE = Constants.R_HEADS + "private";

	private Repository src;

	private Repository dst;

	private RevCommit A, B, P;

	private RevBlob a, b;

	@Override
	@BeforeEach
	public void setUp(TestInfo testInfo) throws Exception {
		super.setUp(testInfo);

		src = createBareRepository();
		addRepoToClose(src);
		dst = createBareRepository();
		addRepoToClose(dst);

		// Fill dst with a some common history.
		//
		try (TestRepository<Repository> d = new TestRepository<>(dst)) {
			dst.incrementOpen();
			a = d.blob("a");
			A = d.commit(d.tree(d.file("a", a)));
			B = d.commit().parent(A).create();
			d.update(R_MASTER, B);

			// Clone from dst into src
			//
			try (Transport t = Transport.open(src, uriOf(dst))) {
				t.fetch(PM,
						Collections.singleton(new RefSpec("+refs/*:refs/*")));
				assertEquals(B, src.resolve(R_MASTER));
			}

			// Now put private stuff into dst.
			//
			b = d.blob("b");
			P = d.commit(d.tree(d.file("b", b)), A);
			d.update(R_PRIVATE, P);
		}
	}

	@Test
	public void testFilterHidesPrivate() throws Exception {
		Map<String, Ref> refs;
		try (TransportLocal t = new TransportLocal(src, uriOf(dst),
				dst.getDirectory()) {
			@Override
			ReceivePack createReceivePack(Repository db) {
				final ReceivePack rp = super.createReceivePack(dst);
				rp.setAdvertiseRefsHook(new HidePrivateHook());
				return rp;
			}
		}) {
			try (PushConnection c = t.openPush()) {
				refs = c.getRefsMap();
			}
		}

		assertNotNull(refs);
		Assertions.assertNull(refs.get(R_PRIVATE));
		Assertions.assertNull(refs.get(Constants.HEAD), "no HEAD");
		assertEquals(1, refs.size());

		Ref master = refs.get(R_MASTER);
		assertNotNull(master);
		assertEquals(B, master.getObjectId());
	}

	@Test
	public void resetsHaves() throws Exception {
		AtomicReference<Set<ObjectId>> haves = new AtomicReference<>();
		try (TransportLocal t = new TransportLocal(src, uriOf(dst),
				dst.getDirectory()) {
			@Override
			ReceivePack createReceivePack(Repository db) {
				ReceivePack rp = super.createReceivePack(dst);
				rp.setAdvertiseRefsHook(new AdvertiseRefsHook() {
					@Override
					public void advertiseRefs(ReceivePack rp2)
							throws IOException {
						rp.setAdvertisedRefs(rp.getRepository().getAllRefs(),
								null);
						new HidePrivateHook().advertiseRefs(rp);
						haves.set(rp.getAdvertisedObjects());
					}

					@Override
					public void advertiseRefs(UploadPack uploadPack)
							throws ServiceMayNotContinueException {
						throw new UnsupportedOperationException();
					}
				});
				return rp;
			}
		}) {
			try (PushConnection c = t.openPush()) {
				// Just has to open/close for advertisement.
			}
		}

		assertEquals(1, haves.get().size());
		assertTrue(haves.get().contains(B));
		assertFalse(haves.get().contains(P));
	}

	private TransportLocal newTransportLocalWithStrictValidation()
			throws Exception {
		return new TransportLocal(src, uriOf(dst), dst.getDirectory()) {
			@Override
			ReceivePack createReceivePack(Repository db) {
				final ReceivePack rp = super.createReceivePack(dst);
				rp.setCheckReceivedObjects(true);
				rp.setCheckReferencedObjectsAreReachable(true);
				rp.setAdvertiseRefsHook(new HidePrivateHook());
				return rp;
			}
		};
	}

	@Test
	public void testSuccess() throws Exception {
		// Manually force a delta of an object so we reuse it later.
		//
		TemporaryBuffer.Heap pack = new TemporaryBuffer.Heap(1024);

		packHeader(pack, 2);
		pack.write((Constants.OBJ_BLOB) << 4 | 1);
		deflate(pack, new byte[] { 'a' });

		pack.write((Constants.OBJ_REF_DELTA) << 4 | 4);
		a.copyRawTo(pack);
		deflate(pack, new byte[] { 0x1, 0x1, 0x1, 'b' });

		digest(pack);
		openPack(pack);

		// Verify the only storage of b is our packed delta above.
		//
		ObjectDirectory od = (ObjectDirectory) src.getObjectDatabase();
		assertTrue(od.has(b));
		assertFalse(od.fileFor(b).exists());

		// Now use b but in a different commit than what is hidden.
		//
		try (TestRepository<Repository> s = new TestRepository<>(src)) {
			src.incrementOpen();
			RevCommit N = s.commit().parent(B).add("q", b).create();
			s.update(R_MASTER, N);

			// Push this new content to the remote, doing strict validation.
			//
			PushResult r;
			RemoteRefUpdate u = new RemoteRefUpdate( //
					src, //
					R_MASTER, // src name
					R_MASTER, // dst name
					false, // do not force update
					null, // local tracking branch
					null // expected id
			);
			try (TransportLocal t = newTransportLocalWithStrictValidation()) {
				t.setPushThin(true);
				r = t.push(PM, Collections.singleton(u));
			}

			assertNotNull(r);
			Assertions.assertNull(r.getAdvertisedRef(R_PRIVATE), "private not advertised");
			Assertions.assertSame(RemoteRefUpdate.Status.OK, u.getStatus(), "master updated");
			assertEquals(N, dst.resolve(R_MASTER));
		}
	}

	@Test
	public void testCreateBranchAtHiddenCommitFails() throws Exception {
		final TemporaryBuffer.Heap pack = new TemporaryBuffer.Heap(64);
		packHeader(pack, 0);
		digest(pack);

		final TemporaryBuffer.Heap inBuf = new TemporaryBuffer.Heap(256);
		final PacketLineOut inPckLine = new PacketLineOut(inBuf);
		inPckLine.writeString(ObjectId.zeroId().name() + ' ' + P.name() + ' '
				+ "refs/heads/s" + '\0'
				+ BasePackPushConnection.CAPABILITY_REPORT_STATUS);
		inPckLine.end();
		pack.writeTo(inBuf, PM);

		final TemporaryBuffer.Heap outBuf = new TemporaryBuffer.Heap(1024);
		final ReceivePack rp = new ReceivePack(dst);
		rp.setCheckReceivedObjects(true);
		rp.setCheckReferencedObjectsAreReachable(true);
		rp.setAdvertiseRefsHook(new HidePrivateHook());
		try {
			receive(rp, inBuf, outBuf);
			Assertions.fail("Expected UnpackException");
		} catch (UnpackException failed) {
			Throwable err = failed.getCause();
            assertInstanceOf(MissingObjectException.class, err);
			MissingObjectException moe = (MissingObjectException) err;
			assertEquals(P, moe.getObjectId());
		}

		final PacketLineIn r = asPacketLineIn(outBuf);
		String master = r.readString();
		int nul = master.indexOf('\0');
		assertTrue(nul > 0);
		assertEquals(B.name() + ' ' + R_MASTER, master.substring(0, nul));
		assertTrue(PacketLineIn.isEnd(r.readString()));

		assertEquals("unpack error Missing commit " + P.name(), r.readString());
		assertEquals("ng refs/heads/s n/a (unpacker error)", r.readString());
		assertTrue(PacketLineIn.isEnd(r.readString()));
	}

	private static void receive(final ReceivePack rp,
			final TemporaryBuffer.Heap inBuf, final TemporaryBuffer.Heap outBuf)
			throws IOException {
		rp.receive(new ByteArrayInputStream(inBuf.toByteArray()), outBuf, null);
	}

	@Test
	public void testUsingHiddenDeltaBaseFails() throws Exception {
		byte[] delta = { 0x1, 0x1, 0x1, 'c' };
		try (TestRepository<Repository> s = new TestRepository<>(src)) {
			src.incrementOpen();
			RevCommit N = s.commit().parent(B)
					.add("q",
							s.blob(BinaryDelta.apply(
									dst.open(b).getCachedBytes(), delta)))
					.create();

			final TemporaryBuffer.Heap pack = new TemporaryBuffer.Heap(1024);
			packHeader(pack, 3);
			copy(pack, src.open(N));
			copy(pack, src.open(s.parseBody(N).getTree()));
			pack.write((Constants.OBJ_REF_DELTA) << 4 | 4);
			b.copyRawTo(pack);
			deflate(pack, delta);
			digest(pack);

			final TemporaryBuffer.Heap inBuf = new TemporaryBuffer.Heap(1024);
			final PacketLineOut inPckLine = new PacketLineOut(inBuf);
			inPckLine.writeString(ObjectId.zeroId().name() + ' ' + N.name()
					+ ' ' + "refs/heads/s" + '\0'
					+ BasePackPushConnection.CAPABILITY_REPORT_STATUS);
			inPckLine.end();
			pack.writeTo(inBuf, PM);

			final TemporaryBuffer.Heap outBuf = new TemporaryBuffer.Heap(1024);
			final ReceivePack rp = new ReceivePack(dst);
			rp.setCheckReceivedObjects(true);
			rp.setCheckReferencedObjectsAreReachable(true);
			rp.setAdvertiseRefsHook(new HidePrivateHook());
			try {
				receive(rp, inBuf, outBuf);
				Assertions.fail("Expected UnpackException");
			} catch (UnpackException failed) {
				Throwable err = failed.getCause();
                assertInstanceOf(MissingObjectException.class, err);
				MissingObjectException moe = (MissingObjectException) err;
				assertEquals(b, moe.getObjectId());
			}

			final PacketLineIn r = asPacketLineIn(outBuf);
			String master = r.readString();
			int nul = master.indexOf('\0');
			assertTrue(nul > 0);
			assertEquals(B.name() + ' ' + R_MASTER, master.substring(0, nul));
			assertTrue(PacketLineIn.isEnd(r.readString()));

			assertEquals("unpack error Missing blob " + b.name(),
					r.readString());
			assertEquals("ng refs/heads/s n/a (unpacker error)",
					r.readString());
			assertTrue(PacketLineIn.isEnd(r.readString()));
		}
	}

	@Test
	public void testUsingHiddenCommonBlobFails() throws Exception {
		// Try to use the 'b' blob that is hidden.
		//
		try (TestRepository<Repository> s = new TestRepository<>(src)) {
			src.incrementOpen();
			RevCommit N = s.commit().parent(B).add("q", s.blob("b")).create();

			// But don't include it in the pack.
			//
			final TemporaryBuffer.Heap pack = new TemporaryBuffer.Heap(1024);
			packHeader(pack, 2);
			copy(pack, src.open(N));
			copy(pack, src.open(s.parseBody(N).getTree()));
			digest(pack);

			final TemporaryBuffer.Heap inBuf = new TemporaryBuffer.Heap(1024);
			final PacketLineOut inPckLine = new PacketLineOut(inBuf);
			inPckLine.writeString(ObjectId.zeroId().name() + ' ' + N.name()
					+ ' ' + "refs/heads/s" + '\0'
					+ BasePackPushConnection.CAPABILITY_REPORT_STATUS);
			inPckLine.end();
			pack.writeTo(inBuf, PM);

			final TemporaryBuffer.Heap outBuf = new TemporaryBuffer.Heap(1024);
			final ReceivePack rp = new ReceivePack(dst);
			rp.setCheckReceivedObjects(true);
			rp.setCheckReferencedObjectsAreReachable(true);
			rp.setAdvertiseRefsHook(new HidePrivateHook());
			try {
				receive(rp, inBuf, outBuf);
				fail("Expected UnpackException");
			} catch (UnpackException failed) {
				Throwable err = failed.getCause();
				assertTrue(err instanceof MissingObjectException);
				MissingObjectException moe = (MissingObjectException) err;
				assertEquals(b, moe.getObjectId());
			}

			final PacketLineIn r = asPacketLineIn(outBuf);
			String master = r.readString();
			int nul = master.indexOf('\0');
			assertTrue( nul > 0);
			assertEquals(B.name() + ' ' + R_MASTER, master.substring(0, nul));
			assertTrue(PacketLineIn.isEnd(r.readString()));

			assertEquals("unpack error Missing blob " + b.name(),
					r.readString());
			assertEquals("ng refs/heads/s n/a (unpacker error)",
					r.readString());
			assertTrue(PacketLineIn.isEnd(r.readString()));
		}
	}

	@Test
	public void testUsingUnknownBlobFails() throws Exception {
		// Try to use the 'n' blob that is not on the server.
		//
		try (TestRepository<Repository> s = new TestRepository<>(src)) {
			src.incrementOpen();
			RevBlob n = s.blob("n");
			RevCommit N = s.commit().parent(B).add("q", n).create();

			// But don't include it in the pack.
			//
			final TemporaryBuffer.Heap pack = new TemporaryBuffer.Heap(1024);
			packHeader(pack, 2);
			copy(pack, src.open(N));
			copy(pack, src.open(s.parseBody(N).getTree()));
			digest(pack);

			final TemporaryBuffer.Heap inBuf = new TemporaryBuffer.Heap(1024);
			final PacketLineOut inPckLine = new PacketLineOut(inBuf);
			inPckLine.writeString(ObjectId.zeroId().name() + ' ' + N.name()
					+ ' ' + "refs/heads/s" + '\0'
					+ BasePackPushConnection.CAPABILITY_REPORT_STATUS);
			inPckLine.end();
			pack.writeTo(inBuf, PM);

			final TemporaryBuffer.Heap outBuf = new TemporaryBuffer.Heap(1024);
			final ReceivePack rp = new ReceivePack(dst);
			rp.setCheckReceivedObjects(true);
			rp.setCheckReferencedObjectsAreReachable(true);
			rp.setAdvertiseRefsHook(new HidePrivateHook());
			try {
				receive(rp, inBuf, outBuf);
				Assertions.fail("Expected UnpackException");
			} catch (UnpackException failed) {
				Throwable err = failed.getCause();
                assertInstanceOf(MissingObjectException.class, err);
				MissingObjectException moe = (MissingObjectException) err;
				assertEquals(n, moe.getObjectId());
			}

			final PacketLineIn r = asPacketLineIn(outBuf);
			String master = r.readString();
			int nul = master.indexOf('\0');
			assertTrue(nul > 0);
			assertEquals(B.name() + ' ' + R_MASTER, master.substring(0, nul));
			assertTrue(PacketLineIn.isEnd(r.readString()));

			assertEquals("unpack error Missing blob " + n.name(),
					r.readString());
			assertEquals("ng refs/heads/s n/a (unpacker error)",
					r.readString());
			assertTrue(PacketLineIn.isEnd(r.readString()));
		}
	}

	@Test
	public void testIncludesInvalidGitmodules() throws Exception {
		final TemporaryBuffer.Heap inBuf = setupSourceRepoInvalidGitmodules();
		final TemporaryBuffer.Heap outBuf = new TemporaryBuffer.Heap(1024);
		final ReceivePack rp = new ReceivePack(dst);
		rp.setCheckReceivedObjects(true);
		rp.setCheckReferencedObjectsAreReachable(true);
		rp.setAdvertiseRefsHook(new HidePrivateHook());
		try {
			receive(rp, inBuf, outBuf);
			fail("Expected UnpackException");
		} catch (UnpackException failed) {
			// Expected
		}

		final PacketLineIn r = asPacketLineIn(outBuf);
		String master = r.readString();
		int nul = master.indexOf('\0');
		assertTrue(nul > 0);
		assertEquals(B.name() + ' ' + R_MASTER, master.substring(0, nul));
		assertTrue(PacketLineIn.isEnd(r.readString()));

		String errorLine = r.readString();
		assertTrue(errorLine.startsWith("unpack error"));
		assertTrue(errorLine.contains("Invalid submodule URL '-"));
		assertEquals("ng refs/heads/s n/a (unpacker error)", r.readString());
		assertTrue(PacketLineIn.isEnd(r.readString()));
	}

	private TemporaryBuffer.Heap setupSourceRepoInvalidGitmodules()
			throws IOException, Exception, MissingObjectException {
		String fakeGitmodules = new StringBuilder()
				.append("[submodule \"test\"]\n")
				.append("    path = xlib\n")
				.append("    url = https://example.com/repo/xlib.git\n\n")
				.append("[submodule \"test2\"]\n")
				.append("    path = zlib\n")
				.append("    url = -upayload.sh\n")
				.toString();

		try (TestRepository<Repository> s = new TestRepository<>(src)) {
			src.incrementOpen();
			RevBlob blob = s.blob(fakeGitmodules);
			RevCommit N = s.commit().parent(B).add(".gitmodules", blob)
					.create();
			RevTree t = s.parseBody(N).getTree();

			final TemporaryBuffer.Heap pack = new TemporaryBuffer.Heap(1024);
			packHeader(pack, 3);
			copy(pack, src.open(N));
			copy(pack, src.open(t));
			copy(pack, src.open(blob));
			digest(pack);

			final TemporaryBuffer.Heap inBuf = new TemporaryBuffer.Heap(1024);
			final PacketLineOut inPckLine = new PacketLineOut(inBuf);
			inPckLine.writeString(ObjectId.zeroId().name() + ' ' + N.name()
					+ ' ' + "refs/heads/s" + '\0'
					+ BasePackPushConnection.CAPABILITY_REPORT_STATUS);
			inPckLine.end();
			pack.writeTo(inBuf, PM);
			return inBuf;
		}
	}

	@Test
	public void testUsingUnknownTreeFails() throws Exception {
		try (TestRepository<Repository> s = new TestRepository<>(src)) {
			src.incrementOpen();
			RevCommit N = s.commit().parent(B).add("q", s.blob("a")).create();
			RevTree t = s.parseBody(N).getTree();

			// Don't include the tree in the pack.
			//
			final TemporaryBuffer.Heap pack = new TemporaryBuffer.Heap(1024);
			packHeader(pack, 1);
			copy(pack, src.open(N));
			digest(pack);

			final TemporaryBuffer.Heap inBuf = new TemporaryBuffer.Heap(1024);
			final PacketLineOut inPckLine = new PacketLineOut(inBuf);
			inPckLine.writeString(ObjectId.zeroId().name() + ' ' + N.name()
					+ ' ' + "refs/heads/s" + '\0'
					+ BasePackPushConnection.CAPABILITY_REPORT_STATUS);
			inPckLine.end();
			pack.writeTo(inBuf, PM);

			final TemporaryBuffer.Heap outBuf = new TemporaryBuffer.Heap(1024);
			final ReceivePack rp = new ReceivePack(dst);
			rp.setCheckReceivedObjects(true);
			rp.setCheckReferencedObjectsAreReachable(true);
			rp.setAdvertiseRefsHook(new HidePrivateHook());
			try {
				receive(rp, inBuf, outBuf);
				Assertions.fail("Expected UnpackException");
			} catch (UnpackException failed) {
				Throwable err = failed.getCause();
                assertInstanceOf(MissingObjectException.class, err);
				MissingObjectException moe = (MissingObjectException) err;
				assertEquals(t, moe.getObjectId());
			}

			final PacketLineIn r = asPacketLineIn(outBuf);
			String master = r.readString();
			int nul = master.indexOf('\0');
			assertTrue(nul > 0);
			assertEquals(B.name() + ' ' + R_MASTER, master.substring(0, nul));
			assertTrue(PacketLineIn.isEnd(r.readString()));

			assertEquals("unpack error Missing tree " + t.name(),
					r.readString());
			assertEquals("ng refs/heads/s n/a (unpacker error)",
					r.readString());
			assertTrue(PacketLineIn.isEnd(r.readString()));
		}
	}

	private static void packHeader(TemporaryBuffer.Heap tinyPack, int cnt)
			throws IOException {
		final byte[] hdr = new byte[8];
		NB.encodeInt32(hdr, 0, 2);
		NB.encodeInt32(hdr, 4, cnt);

		tinyPack.write(Constants.PACK_SIGNATURE);
		tinyPack.write(hdr, 0, 8);
	}

	private static void copy(TemporaryBuffer.Heap tinyPack, ObjectLoader ldr)
			throws IOException {
		final byte[] buf = new byte[64];
		final byte[] content = ldr.getCachedBytes();
		int dataLength = content.length;
		int nextLength = dataLength >>> 4;
		int size = 0;
		buf[size++] = (byte) ((nextLength > 0 ? 0x80 : 0x00)
				| (ldr.getType() << 4) | (dataLength & 0x0F));
		dataLength = nextLength;
		while (dataLength > 0) {
			nextLength >>>= 7;
			buf[size++] = (byte) ((nextLength > 0 ? 0x80 : 0x00) | (dataLength & 0x7F));
			dataLength = nextLength;
		}
		tinyPack.write(buf, 0, size);
		deflate(tinyPack, content);
	}

	private static void deflate(TemporaryBuffer.Heap tinyPack,
			final byte[] content)
			throws IOException {
		final Deflater deflater = new Deflater();
		final byte[] buf = new byte[128];
		deflater.setInput(content, 0, content.length);
		deflater.finish();
		do {
			final int n = deflater.deflate(buf, 0, buf.length);
			if (n > 0)
				tinyPack.write(buf, 0, n);
		} while (!deflater.finished());
	}

	private static void digest(TemporaryBuffer.Heap buf) throws IOException {
		MessageDigest md = Constants.newMessageDigest();
		md.update(buf.toByteArray());
		buf.write(md.digest());
	}

	private ObjectInserter inserter;

	@AfterEach
	public void release() {
		if (inserter != null) {
			inserter.close();
		}
	}

	private void openPack(TemporaryBuffer.Heap buf) throws IOException {
		if (inserter == null)
			inserter = src.newObjectInserter();

		final byte[] raw = buf.toByteArray();
		PackParser p = inserter.newPackParser(new ByteArrayInputStream(raw));
		p.setAllowThin(true);
		p.parse(PM);
	}

	private static PacketLineIn asPacketLineIn(TemporaryBuffer.Heap buf)
			throws IOException {
		return new PacketLineIn(new ByteArrayInputStream(buf.toByteArray()));
	}

	private static final class HidePrivateHook extends AbstractAdvertiseRefsHook {
		@Override
		public Map<String, Ref> getAdvertisedRefs(Repository r, RevWalk revWalk) {
			Map<String, Ref> refs = new HashMap<>(r.getAllRefs());
			assertNotNull(refs.remove(R_PRIVATE));
			return refs;
		}
	}

	private static URIish uriOf(Repository r) throws URISyntaxException {
		return new URIish(r.getDirectory().getAbsolutePath());
	}
}
