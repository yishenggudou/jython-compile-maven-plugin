package com.github.yishenggudou;

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
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal which generates jython-standalone into .
 *
 * @goal jython
 * @phase compile
 */
public class JythonMojo extends AbstractMojo {

    private static final String SETUPTOOLS_EGG = "setuptools-0.6c11-py2.7.egg";

    /**
     * @parameter property="project.build.outputDirectory"
     * @required
     */
    private File outputDirectory;

    /**
     * Libraries needed to include.
     *
     * @parameter
     * @optional
     */
    private List<String> libraries;


    private String mirror;

    /**
     * Caching directory to download and build python packages, as well as
     * extracted jython dir
     *
     * @parameter property="jython.temporaryDirectory"
     * default="target/jython-build-tmp"
     * @optional
     */
    private File temporaryBuildDirectory;

    /**
     * Dependencies. Will be searched for jython
     *
     * @parameter property="plugin.artifacts"
     */
    private List<DefaultArtifact> pluginArtifacts;

    /**
     * Lib/
     */
    private File libdir;

    /**
     * Lib/ in the output
     */
    private File installlibdir;

    /**
     * The Jython dependency
     */
    private DefaultArtifact jythonArtifact;

    /**
     * The setuptools jar resource
     */
    private URL setuptoolsResource;

    /**
     * The setuptools jar, once copied from the resource
     */
    private File setuptoolsJar;

    /**
     * Lib/site-packages
     */
    private File sitepackagesdir;

    /**
     * Where packages are downloaded and built
     */
    private File packageDownloadCacheDir;

    /**
     * Should we override files during extraction if they already exist?
     * <p>
     * if true: will never work on tainted files; if false: will be faster.
     */
    private static final boolean OVERRIDE = false;

    private void setupVariables() throws MojoExecutionException {
        if (temporaryBuildDirectory == null) {
            temporaryBuildDirectory = new File("target/jython-plugins-tmp");
        }
        temporaryBuildDirectory.mkdirs();
        packageDownloadCacheDir = new File(temporaryBuildDirectory, "build");
        packageDownloadCacheDir.mkdir();
        libdir = new File(temporaryBuildDirectory, "Lib");
        installlibdir = new File(outputDirectory, "Lib");

        jythonArtifact = findJythonArtifact();
        if (!jythonArtifact.getFile().getName().endsWith(".jar")) {
            throw new MojoExecutionException("I expected " + jythonArtifact
                    + " to provide a jar, but got " + jythonArtifact.getFile());
        }

        setuptoolsResource = getClass().getResource(SETUPTOOLS_EGG);
        if (setuptoolsResource == null)
            throw new MojoExecutionException(
                    "resource setuptools egg not found");
        setuptoolsJar = new File(packageDownloadCacheDir, SETUPTOOLS_EGG);
        sitepackagesdir = new File(libdir, "site-packages");
    }

    /**
     * Strategy A: include jython in plugin. Extract on the run.
     * <p>
     * Strategy B: Project also has dependency on jython. We find that jar and
     * extract it and work from there.
     * <p>
     * B has the benefit that we don't have to update this plugin for every
     * version and the user needs the jython dependency anyway to call the
     * Python Console
     */
    public void execute() throws MojoExecutionException {
        setupVariables();

        extractJarToDirectory(jythonArtifact.getFile(), temporaryBuildDirectory);

        // now what? we have the jython content, now we need
        // easy_install
        getLog().info("installing easy_install ...");
        try {
            if (OVERRIDE || !setuptoolsJar.exists()) {
                FileUtils.copyInputStreamToFile(
                        setuptoolsResource.openStream(), setuptoolsJar);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("copying setuptools failed", e);
        }
        extractJarToDirectory(setuptoolsJar, new File(sitepackagesdir,
                SETUPTOOLS_EGG));
        try {
            IOUtils.write("./" + SETUPTOOLS_EGG + "\n", new FileOutputStream(
                    new File(sitepackagesdir, "setuptools.pth")));
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "writing path entry for setuptools failed", e);
        }
        getLog().info("installing easy_install done");

        if (libraries == null) {
            getLog().info("no python libraries requested");
        } else {
            getLog().info("installing requested python libraries");
            // then we need to call easy_install to install the other
            // dependencies.
            runJythonScriptOnInstall(temporaryBuildDirectory,
                    getEasyInstallArgs("Lib/site-packages/" + SETUPTOOLS_EGG
                            + "/easy_install.py"));
            getLog().info("installing requested python libraries done");
        }

        getLog().info("copying requested libraries");
        /**
         * we installed the packages into our temporary build directory now we
         * want to move these libraries into outputDirectory/Lib
         *
         * <pre>
         * mv --no-override temporaryBuildDirectory/Lib/site-packages/*.egg/* outputDirectory/Lib/
         * </pre>
         */
        for (File egg : sitepackagesdir.listFiles((FileFilter) new SuffixFileFilter(".egg"))) {
            try {
                FileUtils.deleteDirectory(new File(installlibdir, egg.getName()));
            } catch (IOException e) {
            }
            try {
                // an egg is either a zip file or a directory
                if (egg.isFile()) {
                    getLog().info("extracting " + egg + " into "
                            + new File(outputDirectory, "Lib"));
                    eggExtract(egg, installlibdir);
                } else if (egg.isDirectory()) {
                    getLog().info("copying " + egg + " into "
                            + new File(outputDirectory, "Lib"));
                    FileUtils.copyDirectory(egg, installlibdir);
                }
            } catch (IOException e) {
                throw new MojoExecutionException("copying " + egg + " to "
                        + new File(outputDirectory, "Lib") + " failed", e);
            }
        }

        /**
         * Setuptools installs a site.py for every package. This might conflict
         * with the jython installation. All it does anyway is help look for
         * eggs, so it wouldn't help us. Errors are ignored here.
         */
        new File(installlibdir, "site.py").delete();
        new File(installlibdir, "site$py.class").delete();
        getLog().info("copying requested libraries done");

        /**
         * If the project does not want its python sources to be in Lib/ it
         * needs to call
         *
         * PySystemState.addPaths(path, jarFileName + "/myLibFolder");
         *
         * before starting Python up.
         */
    }

    public void eggExtract(File eggFile, File installlibdir) throws IOException {
        ZipFile f = new ZipFile(eggFile);
        Enumeration<? extends ZipEntry> entries = f.entries();
        while (entries.hasMoreElements()) {
            ZipEntry eggZipEntry = entries.nextElement();
            File extractTo = new File(installlibdir, eggZipEntry.getName());
            if (eggZipEntry.isDirectory()) {
                extractTo.mkdirs();
            } else {
                extractTo.getParentFile().mkdirs();
                InputStream zipEntryContent = f.getInputStream(eggZipEntry);
                try {
                    FileOutputStream contentDestination = new FileOutputStream(extractTo);
                    try {
                        IOUtils.copy(zipEntryContent, contentDestination);
                    } finally {
                        contentDestination.close();
                    }
                } finally {
                    zipEntryContent.close();
                }
            }
        }
    }

    private List<String> getEasyInstallArgs(String easy_install_script)
            throws MojoExecutionException {
        List<String> args = new ArrayList<String>();

        // I want to launch
        args.add("java");
        // to run the generated jython installation here
        args.add("-cp");
        args.add("." + getClassPathSeparator() + "Lib");
        // which should know about itself
        args.add("-Dpython.home=.");
        File jythonFakeExecutable = new File(temporaryBuildDirectory, "jython");
        try {
            jythonFakeExecutable.createNewFile();
        } catch (IOException e) {
            throw new MojoExecutionException("couldn't create file", e);
        }
        args.add("-Dpython.executable=" + jythonFakeExecutable.getName());
        args.add("org.python.util.jython");
        // and it should run easy_install
        args.add(easy_install_script);
        // with some arguments
        // args.add("--optimize");
        // args.add("--install-dir");
        // args.add(outputDirectory.getAbsolutePath());
        // and cache here
        args.add("--build-directory");
        args.add(packageDownloadCacheDir.getAbsolutePath());
        args.add("-i");
        args.add(mirror);
        // and install these libraries
        args.addAll(libraries);

        return args;
    }

    private String getClassPathSeparator() {
        if (File.separatorChar == '\\')
            return ";";
        else
            return ":";
    }

    public void runJythonScriptOnInstall(File outputDirectory, List<String> args)
            throws MojoExecutionException {
        getLog().info("running " + args + " in " + outputDirectory);
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.directory(outputDirectory);
        final Process p;
        try {
            p = pb.start();
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Executing jython failed. tried to run: " + pb.command(), e);
        }
        copyIO(p.getInputStream(), System.out);
        copyIO(p.getErrorStream(), System.err);
        copyIO(System.in, p.getOutputStream());
        try {
            if (p.waitFor() != 0) {
                throw new MojoExecutionException(
                        "Jython failed with return code: " + p.exitValue());
            }
        } catch (InterruptedException e) {
            throw new MojoExecutionException("Python tests were interrupted", e);
        }

    }

    public Collection<File> extractJarToDirectory(File jar, File outputDirectory)
            throws MojoExecutionException {
        getLog().info("extracting " + jar);
        JarFile ja = openJarFile(jar);
        Enumeration<JarEntry> en = ja.entries();
        Collection<File> files = extractAllFiles(outputDirectory, ja, en);
        closeFile(ja);
        return files;
    }

    private JarFile openJarFile(File jar) throws MojoExecutionException {
        try {
            return new JarFile(jar);
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "opening jython artifact jar failed", e);
        }
    }

    private void closeFile(ZipFile ja) throws MojoExecutionException {
        try {
            ja.close();
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "closing jython artifact jar failed", e);
        }
    }

    private Collection<File> extractAllFiles(File outputDirectory, ZipFile ja,
                                             Enumeration<JarEntry> en) throws MojoExecutionException {
        List<File> files = new ArrayList<File>();
        while (en.hasMoreElements()) {
            JarEntry el = en.nextElement();
            // getLog().info(" > " + el);
            if (!el.isDirectory()) {
                File destFile = new File(outputDirectory, el.getName());
                // destFile = new File(outputDirectory, destFile.getName());
                if (OVERRIDE || !destFile.exists()) {
                    destFile.getParentFile().mkdirs();
                    try {
                        FileOutputStream fo = new FileOutputStream(destFile);
                        IOUtils.copy(ja.getInputStream(el), fo);
                        fo.close();
                    } catch (IOException e) {
                        throw new MojoExecutionException("extracting "
                                + el.getName()
                                + " from jython artifact jar failed", e);
                    }
                }
                files.add(destFile);
            }
        }
        return files;
    }

    private DefaultArtifact findJythonArtifact() throws MojoExecutionException {
        for (DefaultArtifact i : pluginArtifacts) {
            if (i.getArtifactId().equals("jython-standalone")
                    && i.getGroupId().equals("org.python")) {
                return i;
            }
        }
        throw new MojoExecutionException(
                "org.python.jython-standalone dependency not found. "
                        + "\n"
                        + "Add a dependency to jython-standalone to your project: \n"
                        + "	<dependency>\n"
                        + "		<groupId>org.python</groupId>\n"
                        + "		<artifactId>jython-standalone</artifactId>\n"
                        + "		<version>2.7.0</version>\n" + "	</dependency>"
                        + "\n");
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
