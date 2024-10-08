/*
 * Copyright (C) 2009, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.revwalk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;

public class FooterLineTest extends RepositoryTestCase {
	@Test
	public void testNoFooters_EmptyBody() {
		String msg = buildMessage("");
		List<FooterLine> footers = FooterLine.fromMessage(msg);
		assertNotNull(footers);
		assertEquals(0, footers.size());
	}

	@Test
	public void testNoFooters_NewlineOnlyBody1() {
		String msg = buildMessage("\n");
		List<FooterLine> footers = FooterLine.fromMessage(msg);
		assertNotNull(footers);
		assertEquals(0, footers.size());
	}

	@Test
	public void testNoFooters_NewlineOnlyBody5() {
		String msg = buildMessage("\n\n\n\n\n");
		List<FooterLine> footers = FooterLine.fromMessage(msg);
		assertNotNull(footers);
		assertEquals(0, footers.size());
	}

	@Test
	public void testNoFooters_OneLineBodyNoLF() {
		String msg = buildMessage("this is a commit");
		List<FooterLine> footers = FooterLine.fromMessage(msg);
		assertNotNull(footers);
		assertEquals(0, footers.size());
	}

	@Test
	public void testNoFooters_OneLineBodyWithLF() {
		String msg = buildMessage("this is a commit\n");
		List<FooterLine> footers = FooterLine.fromMessage(msg);
		assertNotNull(footers);
		assertEquals(0, footers.size());
	}

	@Test
	public void testNoFooters_ShortBodyNoLF() {
		String msg = buildMessage("subject\n\nbody of commit");
		List<FooterLine> footers = FooterLine.fromMessage(msg);
		assertNotNull(footers);
		assertEquals(0, footers.size());
	}

	@Test
	public void testNoFooters_ShortBodyWithLF() {
		String msg = buildMessage("subject\n\nbody of commit\n");
		List<FooterLine> footers = FooterLine.fromMessage(msg);
		assertNotNull(footers);
		assertEquals(0, footers.size());
	}

	@Test
	public void testNoFooters_noRawMsg_SingleLineNoHeaders() {
		String noRawMsg = "commit message with no header lines\n";
		List<FooterLine> footers = FooterLine.fromMessage(noRawMsg);
		assertNotNull(footers);
		assertEquals(0, footers.size());
	}

	@Test
	public void testOneFooter_noRawMsg_MultiParagraphNoHeaders() {
		String noRawMsg = "subject\n\n"
			+ "Not: footer\n\n"
			+ "Footer: value\n";
		List<FooterLine> footers = FooterLine.fromMessage(noRawMsg);
		assertNotNull(footers);
		assertEquals(1, footers.size());
	}

	@Test
	public void testOneFooter_longSubject_NoHeaders() {
		String noRawMsg = "50+ chars loooooooooooooong custom commit message.\n\n"
				+ "Footer: value\n";
		List<FooterLine> footers = FooterLine.fromMessage(noRawMsg);
		assertNotNull(footers);
		assertEquals(1, footers.size());
	}

	@Test
	public void testSignedOffBy_OneUserNoLF() {
		String msg = buildMessage("subject\n\nbody of commit\n" + "\n"
			+ "Signed-off-by: A. U. Thor <a@example.com>");
		List<FooterLine> footers = FooterLine.fromMessage(msg);
		FooterLine f;

		assertNotNull(footers);
		assertEquals(1, footers.size());

		f = footers.get(0);
		assertEquals("Signed-off-by", f.getKey());
		assertEquals("A. U. Thor <a@example.com>", f.getValue());
		assertEquals("a@example.com", f.getEmailAddress());
	}

	@Test
	public void testSignedOffBy_OneUserWithLF() {
		String msg = buildMessage("subject\n\nbody of commit\n" + "\n"
			+ "Signed-off-by: A. U. Thor <a@example.com>\n");
		List<FooterLine> footers = FooterLine.fromMessage(msg);
		FooterLine f;

		assertNotNull(footers);
		assertEquals(1, footers.size());

		f = footers.get(0);
		assertEquals("Signed-off-by", f.getKey());
		assertEquals("A. U. Thor <a@example.com>", f.getValue());
		assertEquals("a@example.com", f.getEmailAddress());
	}

	@Test
	public void testSignedOffBy_IgnoreWhitespace() {
		// We only ignore leading whitespace on the value, trailing
		// is assumed part of the value.
		//
		String msg = buildMessage("subject\n\nbody of commit\n" + "\n"
			+ "Signed-off-by:   A. U. Thor <a@example.com>  \n");
		List<FooterLine> footers = FooterLine.fromMessage(msg);		FooterLine f;

		assertNotNull(footers);
		assertEquals(1, footers.size());

		f = footers.get(0);
		assertEquals("Signed-off-by", f.getKey());
		assertEquals("A. U. Thor <a@example.com>  ", f.getValue());
		assertEquals("a@example.com", f.getEmailAddress());
	}

	@Test
	public void testEmptyValueNoLF() {
		String msg = buildMessage("subject\n\nbody of commit\n" + "\n"
			+ "Signed-off-by:");
		List<FooterLine> footers = FooterLine.fromMessage(msg);
		FooterLine f;

		assertNotNull(footers);
		assertEquals(1, footers.size());

		f = footers.get(0);
		assertEquals("Signed-off-by", f.getKey());
		assertEquals("", f.getValue());
		assertNull(f.getEmailAddress());
	}

	@Test
	public void testEmptyValueWithLF() {
		String msg = buildMessage("subject\n\nbody of commit\n" + "\n"
			+ "Signed-off-by:\n");
		List<FooterLine> footers = FooterLine.fromMessage(msg);
		FooterLine f;

		assertNotNull(footers);
		assertEquals(1, footers.size());

		f = footers.get(0);
		assertEquals("Signed-off-by", f.getKey());
		assertEquals("", f.getValue());
		assertNull(f.getEmailAddress());
	}

	@Test
	public void testShortKey() {
		String msg = buildMessage("subject\n\nbody of commit\n" + "\n"
			+ "K:V\n");
		List<FooterLine> footers = FooterLine.fromMessage(msg);
		FooterLine f;

		assertNotNull(footers);
		assertEquals(1, footers.size());

		f = footers.get(0);
		assertEquals("K", f.getKey());
		assertEquals("V", f.getValue());
		assertNull(f.getEmailAddress());
	}

	@Test
	public void testNonDelimtedEmail() {
		String msg = buildMessage("subject\n\nbody of commit\n" + "\n"
			+ "Acked-by: re@example.com\n");
		List<FooterLine> footers = FooterLine.fromMessage(msg);
		FooterLine f;

		assertNotNull(footers);
		assertEquals(1, footers.size());

		f = footers.get(0);
		assertEquals("Acked-by", f.getKey());
		assertEquals("re@example.com", f.getValue());
		assertEquals("re@example.com", f.getEmailAddress());
	}

	@Test
	public void testNotEmail() {
		String msg = buildMessage("subject\n\nbody of commit\n" + "\n"
			+ "Acked-by: Main Tain Er\n");
		List<FooterLine> footers = FooterLine.fromMessage(msg);
		FooterLine f;

		assertNotNull(footers);
		assertEquals(1, footers.size());

		f = footers.get(0);
		assertEquals("Acked-by", f.getKey());
		assertEquals("Main Tain Er", f.getValue());
		assertNull(f.getEmailAddress());
	}

	@Test
	public void testSignedOffBy_ManyUsers() {
		String msg = buildMessage("subject\n\nbody of commit\n"
			+ "Not-A-Footer-Line: this line must not be read as a footer\n"
			+ "\n" // paragraph break, now footers appear in final block
			+ "Signed-off-by: A. U. Thor <a@example.com>\n"
			+ "CC:            <some.mailing.list@example.com>\n"
			+ "Acked-by: Some Reviewer <sr@example.com>\n"
			+ "Signed-off-by: Main Tain Er <mte@example.com>\n");
		List<FooterLine> footers = FooterLine.fromMessage(msg);
		FooterLine f;

		assertNotNull(footers);
		assertEquals(4, footers.size());

		f = footers.get(0);
		assertEquals("Signed-off-by", f.getKey());
		assertEquals("A. U. Thor <a@example.com>", f.getValue());
		assertEquals("a@example.com", f.getEmailAddress());

		f = footers.get(1);
		assertEquals("CC", f.getKey());
		assertEquals("<some.mailing.list@example.com>", f.getValue());
		assertEquals("some.mailing.list@example.com", f.getEmailAddress());

		f = footers.get(2);
		assertEquals("Acked-by", f.getKey());
		assertEquals("Some Reviewer <sr@example.com>", f.getValue());
		assertEquals("sr@example.com", f.getEmailAddress());

		f = footers.get(3);
		assertEquals("Signed-off-by", f.getKey());
		assertEquals("Main Tain Er <mte@example.com>", f.getValue());
		assertEquals("mte@example.com", f.getEmailAddress());
	}

	@Test
	public void testSignedOffBy_SkipNonFooter() {
		String msg = buildMessage("subject\n\nbody of commit\n"
			+ "Not-A-Footer-Line: this line must not be read as a footer\n"
			+ "\n" // paragraph break, now footers appear in final block
			+ "Signed-off-by: A. U. Thor <a@example.com>\n"
			+ "CC:            <some.mailing.list@example.com>\n"
			+ "not really a footer line but we'll skip it anyway\n"
			+ "Acked-by: Some Reviewer <sr@example.com>\n"
			+ "Signed-off-by: Main Tain Er <mte@example.com>\n");
		List<FooterLine> footers = FooterLine.fromMessage(msg);
		FooterLine f;

		assertNotNull(footers);
		assertEquals(4, footers.size());

		f = footers.get(0);
		assertEquals("Signed-off-by", f.getKey());
		assertEquals("A. U. Thor <a@example.com>", f.getValue());
		assertEquals(217, f.getStartOffset());
		assertEquals(258, f.getEndOffset());

		f = footers.get(1);
		assertEquals("CC", f.getKey());
		assertEquals("<some.mailing.list@example.com>", f.getValue());
		assertEquals(259, f.getStartOffset());
		assertEquals(305, f.getEndOffset());

		f = footers.get(2);
		assertEquals("Acked-by", f.getKey());
		assertEquals("Some Reviewer <sr@example.com>", f.getValue());
		assertEquals(356, f.getStartOffset());
		assertEquals(396, f.getEndOffset());

		f = footers.get(3);
		assertEquals("Signed-off-by", f.getKey());
		assertEquals("Main Tain Er <mte@example.com>", f.getValue());
		assertEquals(397, f.getStartOffset());
		assertEquals(442, f.getEndOffset());
	}

	@Test
	public void testFilterFootersIgnoreCase() {
		String msg = buildMessage("subject\n\nbody of commit\n"
			+ "Not-A-Footer-Line: this line must not be read as a footer\n"
			+ "\n" // paragraph break, now footers appear in final block
			+ "Signed-Off-By: A. U. Thor <a@example.com>\n"
			+ "CC:            <some.mailing.list@example.com>\n"
			+ "Acked-by: Some Reviewer <sr@example.com>\n"
			+ "signed-off-by: Main Tain Er <mte@example.com>\n");
		List<String> footers = FooterLine.getValues(
			FooterLine.fromMessage(msg), "signed-off-by");

		assertNotNull(footers);
		assertEquals(2, footers.size());

		assertEquals("A. U. Thor <a@example.com>", footers.get(0));
		assertEquals("Main Tain Er <mte@example.com>", footers.get(1));
	}

	@Test
	public void testMatchesBugId() {
		String msg = buildMessage("this is a commit subject for test\n"
			+ "\n" // paragraph break, now footers appear in final block
			+ "Simple-Bug-Id: 42\n");
		List<FooterLine> footers = FooterLine.fromMessage(msg);

		assertNotNull(footers);
		assertEquals(1, footers.size());

		FooterLine line = footers.get(0);
		assertNotNull(line);
		assertEquals("Simple-Bug-Id", line.getKey());
		assertEquals("42", line.getValue());

		FooterKey bugid = new FooterKey("Simple-Bug-Id");
		assertTrue("matches Simple-Bug-Id", line.matches(bugid));
		assertFalse("not Signed-off-by", line.matches(FooterKey.SIGNED_OFF_BY));
		assertFalse("not CC", line.matches(FooterKey.CC));
	}

	@Test
	public void testMultilineFooters() {
		String msg = buildMessage("subject\n\nbody of commit\n"
				+ "Not-A-Footer-Line: this line must not be read as a footer\n"
				+ "\n" // paragraph break, now footers appear in final block
				+ "Notes: The change must not be merged until dependency ABC is\n"
				+ " updated.\n"
				+ "CC:            <some.mailing.list@example.com>\n"
				+ "not really a footer line but we'll skip it anyway\n"
				+ "Acked-by: Some Reviewer <sr@example.com>\n");
		List<FooterLine> footers = FooterLine.fromMessage(msg);
		FooterLine f;

		assertNotNull(footers);
		assertEquals(3, footers.size());

		f = footers.get(0);
		assertEquals("Notes", f.getKey());
		assertEquals(
				"The change must not be merged until dependency ABC is updated.",
				f.getValue());

		f = footers.get(1);
		assertEquals("CC", f.getKey());
		assertEquals("<some.mailing.list@example.com>", f.getValue());

		f = footers.get(2);
		assertEquals("Acked-by", f.getKey());
		assertEquals("Some Reviewer <sr@example.com>", f.getValue());
	}

	@Test
	public void testMultilineFooters_multipleWhitespaceAreAllowed() {
		String msg = buildMessage("subject\n\nbody of commit\n"
				+ "Not-A-Footer-Line: this line must not be read as a footer\n"
				+ "\n" // paragraph break, now footers appear in final block
				+ "Notes: The change must not be merged until dependency ABC is\n"
				+ "    updated.\n");
		List<FooterLine> footers = FooterLine.fromMessage(msg);
		FooterLine f;

		assertNotNull(footers);
		assertEquals(1, footers.size());

		f = footers.get(0);
		assertEquals("Notes", f.getKey());
		assertEquals(
				"The change must not be merged until dependency ABC is updated.",
				f.getValue());
	}

	@Test
	public void testFirstLineNeverFooter() {
		String msg = buildMessage(
				String.join("\n", "First-Line: is never a footer", "Foo: ter",
						"1-is: also a footer"));
		List<FooterLine> footers = FooterLine.fromMessage(msg);
		assertNotNull(footers);
		assertEquals(2, footers.size());
	}

	@Test
	public void testLineAfterFooters() {
		String msg = buildMessage(
				String.join("\n", "Subject line: is never a footer", "Foo: ter",
						"1-is: also a footer", "this is not a footer"));
		List<FooterLine> footers = FooterLine.fromMessage(msg);
		assertNotNull(footers);
		assertEquals(2, footers.size());
	}

	private String buildMessage(String msg) {
		StringBuilder buf = new StringBuilder();
		buf.append("tree " + ObjectId.zeroId().name() + "\n");
		buf.append("author A. U. Thor <a@example.com> 1 +0000\n");
		buf.append("committer A. U. Thor <a@example.com> 1 +0000\n");
		buf.append("\n");
		buf.append(msg);
		return buf.toString();
	}
}
