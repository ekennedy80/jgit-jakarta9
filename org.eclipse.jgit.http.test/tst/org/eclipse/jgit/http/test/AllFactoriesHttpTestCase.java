/*
 * Copyright (C) 2019, Thomas Wolf <thomas.wolf@paranor.ch> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.http.test;

import org.eclipse.jgit.junit.http.HttpTestCase;
import org.eclipse.jgit.transport.HttpTransport;
import org.eclipse.jgit.transport.http.HttpConnectionFactory;
import org.eclipse.jgit.transport.http.JDKHttpConnectionFactory;
import org.eclipse.jgit.transport.http.apache.HttpClientConnectionFactory;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;
import java.util.Collection;

/**
 * Abstract test base class for running HTTP-related tests with all connection
 * factories provided in JGit: the JDK {@link JDKHttpConnectionFactory} and the
 * Apache HTTP {@link HttpClientConnectionFactory}.
 */
@Ignore
//@RunWith(Parameterized.class)
public abstract class AllFactoriesHttpTestCase extends HttpTestCase {

//	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		// run all tests with both connection factories we have
		return Arrays.asList(new Object[][] { { new JDKHttpConnectionFactory() {
			@Override
			public String toString() {
				return this.getClass().getSuperclass().getName();
			}
		} }, { new HttpClientConnectionFactory() {
			@Override
			public String toString() {
				return this.getClass().getSuperclass().getName();
			}
		} } });
	}

	protected AllFactoriesHttpTestCase(HttpConnectionFactory cf) {
		HttpTransport.setConnectionFactory(cf);
	}

	private static HttpConnectionFactory originalFactory;

	@BeforeEach
	public void saveConnectionFactory() {
		originalFactory = HttpTransport.getConnectionFactory();
	}

	@AfterEach
	public void restoreConnectionFactory() {
		HttpTransport.setConnectionFactory(originalFactory);
	}

}
