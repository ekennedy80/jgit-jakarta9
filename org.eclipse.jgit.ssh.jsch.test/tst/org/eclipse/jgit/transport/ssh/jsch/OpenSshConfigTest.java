/*
 * Copyright (C) 2008, 2021 Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.transport.ssh.jsch;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.SshConstants;
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig.Host;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.SystemReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jcraft.jsch.ConfigRepository;
import com.jcraft.jsch.ConfigRepository.Config;

public class OpenSshConfigTest extends RepositoryTestCase {
	private File home;

	private File configFile;

	private OpenSshConfig osc;

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();

		home = new File(trash, "home");
		FileUtils.mkdir(home);

		configFile = new File(new File(home, ".ssh"), Constants.CONFIG);
		FileUtils.mkdir(configFile.getParentFile());

		mockSystemReader.setProperty(Constants.OS_USER_NAME_KEY, "jex_junit");
		mockSystemReader.setProperty("TST_VAR", "TEST");
		osc = new OpenSshConfig(home, configFile);
	}

	private void config(String data) throws IOException {
		FS fs = FS.DETECTED;
		long resolution = FS.getFileStoreAttributes(configFile.toPath())
				.getFsTimestampResolution().toNanos();
		Instant lastMtime = fs.lastModifiedInstant(configFile);
		do {
			try (final OutputStreamWriter fw = new OutputStreamWriter(
					new FileOutputStream(configFile), UTF_8)) {
				fw.write(data);
				TimeUnit.NANOSECONDS.sleep(resolution);
			} catch (InterruptedException e) {
				Thread.interrupted();
			}
		} while (lastMtime.equals(fs.lastModifiedInstant(configFile)));
	}

	@Test
	public void testNoConfig() {
		final Host h = osc.lookup("repo.or.cz");
		assertNotNull(h);
		assertEquals("repo.or.cz", h.getHostName());
		assertEquals("jex_junit", h.getUser());
		assertEquals(22, h.getPort());
		assertEquals(1, h.getConnectionAttempts());
		assertNull(h.getIdentityFile());
	}

	@Test
	public void testSeparatorParsing() throws Exception {
		config("Host\tfirst\n" +
		       "\tHostName\tfirst.tld\n" +
		       "\n" +
		       "Host second\n" +
		       " HostName\tsecond.tld\n" +
		       "Host=third\n" +
		       "HostName=third.tld\n\n\n" +
		       "\t Host = fourth\n\n\n" +
		       " \t HostName\t=fourth.tld\n" +
		       "Host\t =     last\n" +
		       "HostName  \t    last.tld");
		assertNotNull(osc.lookup("first"));
		assertEquals("first.tld", osc.lookup("first").getHostName());
		assertNotNull(osc.lookup("second"));
		assertEquals("second.tld", osc.lookup("second").getHostName());
		assertNotNull(osc.lookup("third"));
		assertEquals("third.tld", osc.lookup("third").getHostName());
		assertNotNull(osc.lookup("fourth"));
		assertEquals("fourth.tld", osc.lookup("fourth").getHostName());
		assertNotNull(osc.lookup("last"));
		assertEquals("last.tld", osc.lookup("last").getHostName());
	}

	@Test
	public void testQuoteParsing() throws Exception {
		config("Host \"good\"\n" +
			" HostName=\"good.tld\"\n" +
			" Port=\"6007\"\n" +
			" User=\"gooduser\"\n" +
			"Host multiple unquoted and \"quoted\" \"hosts\"\n" +
			" Port=\"2222\"\n" +
			"Host \"spaced\"\n" +
			"# Bad host name, but testing preservation of spaces\n" +
			" HostName=\" spaced\ttld \"\n" +
			"# Misbalanced quotes\n" +
			"Host \"bad\"\n" +
			"# OpenSSH doesn't allow this but ...\n" +
			" HostName=bad.tld\"\n");
		assertEquals("good.tld", osc.lookup("good").getHostName());
		assertEquals("gooduser", osc.lookup("good").getUser());
		assertEquals(6007, osc.lookup("good").getPort());
		assertEquals(2222, osc.lookup("multiple").getPort());
		assertEquals(2222, osc.lookup("quoted").getPort());
		assertEquals(2222, osc.lookup("and").getPort());
		assertEquals(2222, osc.lookup("unquoted").getPort());
		assertEquals(2222, osc.lookup("hosts").getPort());
		assertEquals(" spaced\ttld ", osc.lookup("spaced").getHostName());
		assertEquals("bad.tld", osc.lookup("bad").getHostName());
	}

	@Test
	public void testCaseInsensitiveKeyLookup() throws Exception {
		config("Host orcz\n" + "Port 29418\n"
				+ "\tHostName repo.or.cz\nStrictHostKeyChecking yes\n");
		final Host h = osc.lookup("orcz");
		Config c = h.getConfig();
		String exactCase = c.getValue("StrictHostKeyChecking");
		assertEquals("yes", exactCase);
		assertEquals(exactCase, c.getValue("stricthostkeychecking"));
		assertEquals(exactCase, c.getValue("STRICTHOSTKEYCHECKING"));
		assertEquals(exactCase, c.getValue("sTrIcThostKEYcheckING"));
		assertNull(c.getValue("sTrIcThostKEYcheckIN"));
	}

	@Test
	public void testAlias_DoesNotMatch() throws Exception {
		config("Host orcz\n" + "Port 29418\n" + "\tHostName repo.or.cz\n");
		final Host h = osc.lookup("repo.or.cz");
		assertNotNull(h);
		assertEquals("repo.or.cz", h.getHostName());
		assertEquals("jex_junit", h.getUser());
		assertEquals(22, h.getPort());
		assertNull(h.getIdentityFile());
		final Host h2 = osc.lookup("orcz");
		assertEquals("repo.or.cz", h.getHostName());
		assertEquals("jex_junit", h.getUser());
		assertEquals(29418, h2.getPort());
		assertNull(h.getIdentityFile());
	}

	@Test
	public void testAlias_OptionsSet() throws Exception {
		config("Host orcz\n" + "\tHostName repo.or.cz\n" + "\tPort 2222\n"
				+ "\tUser jex\n" + "\tIdentityFile .ssh/id_jex\n"
				+ "\tForwardX11 no\n");
		final Host h = osc.lookup("orcz");
		assertNotNull(h);
		assertEquals("repo.or.cz", h.getHostName());
		assertEquals("jex", h.getUser());
		assertEquals(2222, h.getPort());
		assertEquals(new File(home, ".ssh/id_jex"), h.getIdentityFile());
	}

	@Test
	public void testAlias_OptionsKeywordCaseInsensitive() throws Exception {
		config("hOsT orcz\n" + "\thOsTnAmE repo.or.cz\n" + "\tPORT 2222\n"
				+ "\tuser jex\n" + "\tidentityfile .ssh/id_jex\n"
				+ "\tForwardX11 no\n");
		final Host h = osc.lookup("orcz");
		assertNotNull(h);
		assertEquals("repo.or.cz", h.getHostName());
		assertEquals("jex", h.getUser());
		assertEquals(2222, h.getPort());
		assertEquals(new File(home, ".ssh/id_jex"), h.getIdentityFile());
	}

	@Test
	public void testAlias_OptionsInherit() throws Exception {
		config("Host orcz\n" + "\tHostName repo.or.cz\n" + "\n" + "Host *\n"
				+ "\tHostName not.a.host.example.com\n" + "\tPort 2222\n"
				+ "\tUser jex\n" + "\tIdentityFile .ssh/id_jex\n"
				+ "\tForwardX11 no\n");
		final Host h = osc.lookup("orcz");
		assertNotNull(h);
		assertEquals("repo.or.cz", h.getHostName());
		assertEquals("jex", h.getUser());
		assertEquals(2222, h.getPort());
		assertEquals(new File(home, ".ssh/id_jex"), h.getIdentityFile());
	}

	@Test
	public void testAlias_PreferredAuthenticationsDefault() throws Exception {
		final Host h = osc.lookup("orcz");
		assertNotNull(h);
		assertNull(h.getPreferredAuthentications());
	}

	@Test
	public void testAlias_PreferredAuthentications() throws Exception {
		config("Host orcz\n" + "\tPreferredAuthentications publickey\n");
		final Host h = osc.lookup("orcz");
		assertNotNull(h);
		assertEquals("publickey", h.getPreferredAuthentications());
	}

	@Test
	public void testAlias_InheritPreferredAuthentications() throws Exception {
		config("Host orcz\n" + "\tHostName repo.or.cz\n" + "\n" + "Host *\n"
				+ "\tPreferredAuthentications 'publickey, hostbased'\n");
		final Host h = osc.lookup("orcz");
		assertNotNull(h);
		assertEquals("publickey,hostbased", h.getPreferredAuthentications());
	}

	@Test
	public void testAlias_BatchModeDefault() throws Exception {
		final Host h = osc.lookup("orcz");
		assertNotNull(h);
		assertFalse(h.isBatchMode());
	}

	@Test
	public void testAlias_BatchModeYes() throws Exception {
		config("Host orcz\n" + "\tBatchMode yes\n");
		final Host h = osc.lookup("orcz");
		assertNotNull(h);
		assertTrue(h.isBatchMode());
	}

	@Test
	public void testAlias_InheritBatchMode() throws Exception {
		config("Host orcz\n" + "\tHostName repo.or.cz\n" + "\n" + "Host *\n"
				+ "\tBatchMode yes\n");
		final Host h = osc.lookup("orcz");
		assertNotNull(h);
		assertTrue(h.isBatchMode());
	}

	@Test
	public void testAlias_ConnectionAttemptsDefault() throws Exception {
		final Host h = osc.lookup("orcz");
		assertNotNull(h);
		assertEquals(1, h.getConnectionAttempts());
	}

	@Test
	public void testAlias_ConnectionAttempts() throws Exception {
		config("Host orcz\n" + "\tConnectionAttempts 5\n");
		final Host h = osc.lookup("orcz");
		assertNotNull(h);
		assertEquals(5, h.getConnectionAttempts());
	}

	@Test
	public void testAlias_invalidConnectionAttempts() throws Exception {
		config("Host orcz\n" + "\tConnectionAttempts -1\n");
		final Host h = osc.lookup("orcz");
		assertNotNull(h);
		assertEquals(1, h.getConnectionAttempts());
	}

	@Test
	public void testAlias_badConnectionAttempts() throws Exception {
		config("Host orcz\n" + "\tConnectionAttempts xxx\n");
		final Host h = osc.lookup("orcz");
		assertNotNull(h);
		assertEquals(1, h.getConnectionAttempts());
	}

	@Test
	public void testDefaultBlock() throws Exception {
		config("ConnectionAttempts 5\n\nHost orcz\nConnectionAttempts 3\n");
		final Host h = osc.lookup("orcz");
		assertNotNull(h);
		assertEquals(5, h.getConnectionAttempts());
	}

	@Test
	public void testHostCaseInsensitive() throws Exception {
		config("hOsT orcz\nConnectionAttempts 3\n");
		final Host h = osc.lookup("orcz");
		assertNotNull(h);
		assertEquals(3, h.getConnectionAttempts());
	}

	@Test
	public void testListValueSingle() throws Exception {
		config("Host orcz\nUserKnownHostsFile /foo/bar\n");
		final ConfigRepository.Config c = osc.getConfig("orcz");
		assertNotNull(c);
		assertEquals("/foo/bar", c.getValue("UserKnownHostsFile"));
	}

	@Test
	public void testListValueMultiple() throws Exception {
		// Tilde expansion occurs within the parser
		config("Host orcz\nUserKnownHostsFile \"~/foo/ba z\" /foo/bar \n");
		final ConfigRepository.Config c = osc.getConfig("orcz");
		assertNotNull(c);
		assertArrayEquals(new Object[] { new File(home, "foo/ba z").getPath(),
				"/foo/bar" },
				c.getValues("UserKnownHostsFile"));
	}

	@Test
	public void testRepeatedLookupsWithModification() throws Exception {
		config("Host orcz\n" + "\tConnectionAttempts -1\n");
		final Host h1 = osc.lookup("orcz");
		assertNotNull(h1);
		assertEquals(1, h1.getConnectionAttempts());
		config("Host orcz\n" + "\tConnectionAttempts 5\n");
		final Host h2 = osc.lookup("orcz");
		assertNotNull(h2);
		assertNotSame(h1, h2);
		assertEquals(5, h2.getConnectionAttempts());
		assertEquals(1, h1.getConnectionAttempts());
		assertNotSame(h1.getConfig(), h2.getConfig());
	}

	@Test
	public void testIdentityFile() throws Exception {
		config("Host orcz\nIdentityFile \"~/foo/ba z\"\nIdentityFile /foo/bar");
		final Host h = osc.lookup("orcz");
		assertNotNull(h);
		File f = h.getIdentityFile();
		assertNotNull(f);
		// Host does tilde replacement
		assertEquals(new File(home, "foo/ba z"), f);
		final ConfigRepository.Config c = h.getConfig();
		// Config does tilde replacement, too
		assertArrayEquals(new Object[] { new File(home, "foo/ba z").getPath(),
				"/foo/bar" },
				c.getValues("IdentityFile"));
	}

	@Test
	public void testMultiIdentityFile() throws Exception {
		config("IdentityFile \"~/foo/ba z\"\nHost orcz\nIdentityFile /foo/bar\nHOST *\nIdentityFile /foo/baz");
		final Host h = osc.lookup("orcz");
		assertNotNull(h);
		File f = h.getIdentityFile();
		assertNotNull(f);
		// Host does tilde replacement
		assertEquals(new File(home, "foo/ba z"), f);
		final ConfigRepository.Config c = h.getConfig();
		// Config does tilde replacement, too
		assertArrayEquals(new Object[] { new File(home, "foo/ba z").getPath(),
				"/foo/bar", "/foo/baz" },
				c.getValues("IdentityFile"));
	}

	@Test
	public void testNegatedPattern() throws Exception {
		config("Host repo.or.cz\nIdentityFile ~/foo/bar\nHOST !*.or.cz\nIdentityFile /foo/baz");
		final Host h = osc.lookup("repo.or.cz");
		assertNotNull(h);
		assertEquals(new File(home, "foo/bar"), h.getIdentityFile());
		assertArrayEquals(new Object[] { new File(home, "foo/bar").getPath() },
				h.getConfig().getValues("IdentityFile"));
	}

	@Test
	public void testPattern() throws Exception {
		config("Host repo.or.cz\nIdentityFile ~/foo/bar\nHOST *.or.cz\nIdentityFile /foo/baz");
		final Host h = osc.lookup("repo.or.cz");
		assertNotNull(h);
		assertEquals(new File(home, "foo/bar"), h.getIdentityFile());
		assertArrayEquals(new Object[] { new File(home, "foo/bar").getPath(),
				"/foo/baz" },
				h.getConfig().getValues("IdentityFile"));
	}

	@Test
	public void testMultiHost() throws Exception {
		config("Host orcz *.or.cz\nIdentityFile ~/foo/bar\nHOST *.or.cz\nIdentityFile /foo/baz");
		final Host h1 = osc.lookup("repo.or.cz");
		assertNotNull(h1);
		assertEquals(new File(home, "foo/bar"), h1.getIdentityFile());
		assertArrayEquals(new Object[] { new File(home, "foo/bar").getPath(),
				"/foo/baz" },
				h1.getConfig().getValues("IdentityFile"));
		final Host h2 = osc.lookup("orcz");
		assertNotNull(h2);
		assertEquals(new File(home, "foo/bar"), h2.getIdentityFile());
		assertArrayEquals(new Object[] { new File(home, "foo/bar").getPath() },
				h2.getConfig().getValues("IdentityFile"));
	}

	@Test
	public void testEqualsSign() throws Exception {
		config("Host=orcz\n\tConnectionAttempts = 5\n\tUser=\t  foobar\t\n");
		final Host h = osc.lookup("orcz");
		assertNotNull(h);
		assertEquals(5, h.getConnectionAttempts());
		assertEquals("foobar", h.getUser());
	}

	@Test
	public void testMissingArgument() throws Exception {
		config("Host=orcz\n\tSendEnv\nIdentityFile\t\nForwardX11\n\tUser=\t  foobar\t\n");
		final Host h = osc.lookup("orcz");
		assertNotNull(h);
		assertEquals("foobar", h.getUser());
		assertArrayEquals(new String[0], h.getConfig().getValues("SendEnv"));
		assertNull(h.getIdentityFile());
		assertNull(h.getConfig().getValue("ForwardX11"));
	}

	@Test
	public void testHomeDirUserReplacement() throws Exception {
		config("Host=orcz\n\tIdentityFile %d/.ssh/%u_id_dsa");
		final Host h = osc.lookup("orcz");
		assertNotNull(h);
		assertEquals(new File(new File(home, ".ssh"), "jex_junit_id_dsa"),
				h.getIdentityFile());
	}

	@Test
	public void testHostnameReplacement() throws Exception {
		config("Host=orcz\nHost *.*\n\tHostname %h\nHost *\n\tHostname %h.example.org");
		final Host h = osc.lookup("orcz");
		assertNotNull(h);
		assertEquals("orcz.example.org", h.getHostName());
	}

	@Test
	public void testRemoteUserReplacement() throws Exception {
		config("Host=orcz\n\tUser foo\n" + "Host *.*\n\tHostname %h\n"
				+ "Host *\n\tHostname %h.ex%%20ample.org\n\tIdentityFile ~/.ssh/%h_%r_id_dsa");
		final Host h = osc.lookup("orcz");
		assertNotNull(h);
		assertEquals(
				new File(new File(home, ".ssh"),
						"orcz.ex%20ample.org_foo_id_dsa"),
				h.getIdentityFile());
	}

	@Test
	public void testLocalhostFQDNReplacement() throws Exception {
		String localhost = SystemReader.getInstance().getHostname();
		config("Host=orcz\n\tIdentityFile ~/.ssh/%l_id_dsa");
		final Host h = osc.lookup("orcz");
		assertNotNull(h);
		assertEquals(
				new File(new File(home, ".ssh"), localhost + "_id_dsa"),
				h.getIdentityFile());
	}

	@Test
	public void testPubKeyAcceptedAlgorithms() throws Exception {
		config("Host=orcz\n\tPubkeyAcceptedAlgorithms ^ssh-rsa");
		Host h = osc.lookup("orcz");
		Config c = h.getConfig();
		assertEquals("^ssh-rsa",
				c.getValue(SshConstants.PUBKEY_ACCEPTED_ALGORITHMS));
		assertEquals("^ssh-rsa", c.getValue("PubkeyAcceptedKeyTypes"));
	}

	@Test
	public void testPubKeyAcceptedKeyTypes() throws Exception {
		config("Host=orcz\n\tPubkeyAcceptedKeyTypes ^ssh-rsa");
		Host h = osc.lookup("orcz");
		Config c = h.getConfig();
		assertEquals("^ssh-rsa",
				c.getValue(SshConstants.PUBKEY_ACCEPTED_ALGORITHMS));
		assertEquals("^ssh-rsa", c.getValue("PubkeyAcceptedKeyTypes"));
	}

	@Test
	public void testEolComments() throws Exception {
		config("#Comment\nHost=orcz #Comment\n\tPubkeyAcceptedAlgorithms ^ssh-rsa # Comment\n#Comment");
		Host h = osc.lookup("orcz");
		assertNotNull(h);
		Config c = h.getConfig();
		assertEquals("^ssh-rsa",
				c.getValue(SshConstants.PUBKEY_ACCEPTED_ALGORITHMS));
	}

	@Test
	public void testEnVarSubstitution() throws Exception {
		config("Host orcz\nIdentityFile /tmp/${TST_VAR}\n"
				+ "CertificateFile /tmp/${}/foo\nUser ${TST_VAR}\nIdentityAgent /tmp/${TST_VAR/bar");
		Host h = osc.lookup("orcz");
		assertNotNull(h);
		Config c = h.getConfig();
		assertEquals("/tmp/TEST",
				c.getValue(SshConstants.IDENTITY_FILE));
		// No variable name
		assertEquals("/tmp/${}/foo", c.getValue(SshConstants.CERTIFICATE_FILE));
		// User doesn't get env var substitution:
		assertEquals("${TST_VAR}", c.getValue(SshConstants.USER));
		assertEquals("${TST_VAR}", h.getUser());
		// Unterminated:
		assertEquals("/tmp/${TST_VAR/bar",
				c.getValue(SshConstants.IDENTITY_AGENT));
	}

	@Test
	public void testNegativeMatch() throws Exception {
		config("Host foo.bar !foobar.baz *.baz\n" + "Port 29418\n");
		Host h = osc.lookup("foo.bar");
		assertNotNull(h);
		assertEquals(29418, h.getPort());
		h = osc.lookup("foobar.baz");
		assertNotNull(h);
		assertEquals(22, h.getPort());
		h = osc.lookup("foo.baz");
		assertNotNull(h);
		assertEquals(29418, h.getPort());
	}

	@Test
	public void testNegativeMatch2() throws Exception {
		// Negative match after the positive match.
		config("Host foo.bar *.baz !foobar.baz\n" + "Port 29418\n");
		Host h = osc.lookup("foo.bar");
		assertNotNull(h);
		assertEquals(29418, h.getPort());
		h = osc.lookup("foobar.baz");
		assertNotNull(h);
		assertEquals(22, h.getPort());
		h = osc.lookup("foo.baz");
		assertNotNull(h);
		assertEquals(29418, h.getPort());
	}

	@Test
	public void testNoMatch() throws Exception {
		config("Host !host1 !host2\n" + "Port 29418\n");
		Host h = osc.lookup("host1");
		assertNotNull(h);
		assertEquals(22, h.getPort());
		h = osc.lookup("host2");
		assertNotNull(h);
		assertEquals(22, h.getPort());
		h = osc.lookup("host3");
		assertNotNull(h);
		assertEquals(22, h.getPort());
	}

	@Test
	public void testMultipleMatch() throws Exception {
		config("Host foo.bar\nPort 29418\nIdentityFile /foo\n\n"
				+ "Host *.bar\nPort 22\nIdentityFile /bar\n"
				+ "Host foo.bar\nPort 47\nIdentityFile /baz\n");
		Host h = osc.lookup("foo.bar");
		assertNotNull(h);
		assertEquals(29418, h.getPort());
		assertArrayEquals(new Object[] { "/foo", "/bar", "/baz" },
				h.getConfig().getValues("IdentityFile"));
	}

	@Test
	public void testWhitespace() throws Exception {
		config("Host foo \tbar   baz\nPort 29418\n");
		Host h = osc.lookup("foo");
		assertNotNull(h);
		assertEquals(29418, h.getPort());
		h = osc.lookup("bar");
		assertNotNull(h);
		assertEquals(29418, h.getPort());
		h = osc.lookup("baz");
		assertNotNull(h);
		assertEquals(29418, h.getPort());
		h = osc.lookup("\tbar");
		assertNotNull(h);
		assertEquals(22, h.getPort());
	}
}
