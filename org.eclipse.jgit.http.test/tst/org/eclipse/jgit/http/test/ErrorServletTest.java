/*
 * Copyright (C) 2009-2010, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.http.test;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jgit.http.server.glue.ErrorServlet;
import org.eclipse.jgit.junit.http.AppServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.HttpURLConnection;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ErrorServletTest {
	private AppServer server;

	@BeforeEach
	public void setUp() throws Exception {

		server = new AppServer();

		ServletContextHandler ctx = server.addContext("/");
		ctx.addServlet(new ServletHolder(new ErrorServlet(404)), "/404");
		ctx.addServlet(new ServletHolder(new ErrorServlet(500)), "/500");

		server.setUp();
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (server != null) {
			server.tearDown();
		}
	}

	@Test
	public void testHandler() throws Exception {
		final URI uri = server.getURI();
		assertEquals(404, ((HttpURLConnection) uri.resolve("/404").toURL()
				.openConnection()).getResponseCode());
	}
}
