package com.github.yishenggudou.test;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal which runs an external program to test the python scripts.
 * 
 * @goal python
 * 
 * @phase test
 */
public class PythonTestMojo extends AbstractMojo {
	/**
	 * @parameter property="project.build.testOutputDirectory"
	 * @required
	 */
	private File testOutputDirectory;
	/**
	 * @parameter property="project.build.outputDirectory"
	 * @required
	 */
	private File outputDirectory;

	/**
	 * @parameter property="project.build.scriptSourceDirectory"
	 * @required
	 */
	private File scriptDirectory;

	/**
	 * Executable program to run for test.
	 * 
	 * @parameter property="jython-test.program" default-value="nose"
	 */
	private String program;

	public void execute() throws MojoExecutionException {
		// all we have to do is to run nose on the source directory
		List<String> l = new ArrayList<String>();
		if (program.equals("nose")) {
			l.add("nosetests.bat");
			l.add("--failure-detail");
			l.add("--verbose");
		} else {
			l.add(program);
		}

		ProcessBuilder pb = new ProcessBuilder(l);
		pb.directory(testOutputDirectory);
		pb.environment().put("JYTHONPATH",
				".;" + outputDirectory.getAbsolutePath());
		final Process p;
		getLog().info("starting python tests");
		getLog().info("executing " + pb.command());
		getLog().info("in directory " + testOutputDirectory);
		getLog().info("and also including " + outputDirectory);
		try {
			p = pb.start();
		} catch (IOException e) {
			throw new MojoExecutionException(
					"Python tests execution failed. Provide the executable '"
							+ program + "' in the path", e);
		}
		copyIO(p.getInputStream(), System.out);
		copyIO(p.getErrorStream(), System.err);
		copyIO(System.in, p.getOutputStream());
		try {
			if (p.waitFor() != 0) {
				throw new MojoExecutionException(
						"Python tests failed with return code: "
								+ p.exitValue());
			} else {
				getLog().info("Python tests (" + program + ") succeeded.");
			}
		} catch (InterruptedException e) {
			throw new MojoExecutionException("Python tests were interrupted", e);
		}
	}

	private void copyIO(final InputStream input, final OutputStream output) {
		new Thread(new Runnable() {
			public void run() {
				try {
					IOUtils.copy(input, output);
				} catch (IOException e) {
					getLog().error(e);
				}
			}
		}).start();

	}
}
