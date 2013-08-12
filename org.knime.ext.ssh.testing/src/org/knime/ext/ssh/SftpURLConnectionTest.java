/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * Created on 18.03.2013 by meinl
 */
package org.knime.ext.ssh;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.knime.core.util.FileUtil;

import com.jcraft.jsch.JSch;

/**
 * Testcase for {@link SftpURLConnection}. It assumes that the handler is already registered for the sftp protocol.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
@SuppressWarnings("javadoc")
public class SftpURLConnectionTest {
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private static String strictHostChecking;

    @BeforeClass
    public static void setup() {
        strictHostChecking = JSch.getConfig("StrictHostKeyChecking");
        JSch.setConfig("StrictHostKeyChecking", "no");
    }

    @AfterClass
    public static void tearDown() {
        JSch.setConfig("StrictHostKeyChecking", strictHostChecking);
    }

    /**
     * Checks if the input stream via the sftp protocol works properly.
     *
     * @throws Exception in an exception occurs
     */
    @Test
    public void testRead() throws Exception {
        File tempFile = File.createTempFile("SFTPURLTest", ".txt");
        tempFile.deleteOnExit();

        String writtenContents = new Date().toString();
        FileWriter out = new FileWriter(tempFile);
        out.write(writtenContents);
        out.close();

        String path = tempFile.getAbsoluteFile().toURI().getPath();
        URL url = new URL("sftp://" + System.getProperty("user.name") + "@localhost" + path);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String readContents = in.readLine();
        in.close();
        assertThat("Unexpected data read via sftp protocol", readContents, is(writtenContents));
    }

    /**
     * Checks if the output stream via the sftp protocol works properly.
     *
     * @throws Exception in an exception occurs
     */
    @Test
    public void testWrite() throws Exception {
        File tempFile = File.createTempFile("SFTPURLTest", ".txt");
        tempFile.deleteOnExit();

        String writtenContents = new Date().toString();
        String path = tempFile.getAbsoluteFile().toURI().getPath();
        URL url = new URL("sftp://" + System.getProperty("user.name") + "@localhost" + path);
        URLConnection conn = url.openConnection();
        OutputStream out = conn.getOutputStream();
        out.write(writtenContents.getBytes());
        out.flush();
        out.close();

        BufferedReader in = new BufferedReader(new FileReader(tempFile));
        String readContents = in.readLine();
        in.close();
        assertThat("Unexpected data written via sftp protocol", readContents, is(writtenContents));
    }

    @Test
    public void testWriteToDirectory() throws IOException {
        File dir = FileUtil.createTempDir("SFTPURLTest");

        String path = dir.getAbsoluteFile().toURI().getPath();
        URL url = new URL("sftp://" + System.getProperty("user.name") + "@localhost" + path);
        URLConnection conn = url.openConnection();
        expectedEx.expect(IOException.class);
        expectedEx.expectMessage("Cannot write to a directory");
        conn.getOutputStream();
    }

    @Test
    public void testDate() throws Exception {
        File tempFile = File.createTempFile("SFTPURLTest", ".txt");
        tempFile.deleteOnExit();


        String path = tempFile.getAbsoluteFile().toURI().getPath();
        URL url = new URL("sftp://" + System.getProperty("user.name") + "@localhost" + path);
        URLConnection conn = url.openConnection();
        assertThat("Modification date without connect is not 0", conn.getDate(), is(0L));
        conn.connect();
        // the date retrieved via sftp only has a resolution of seconds, therefore we eliminate the milliseconds
        assertThat("Modification date retrieved via sftp is wrong", conn.getDate() / 1000,
                   is(tempFile.lastModified() / 1000));
    }

    /**
     * Checks if the input stream via the sftp protocol works properly.
     *
     * @throws Exception in an exception occurs
     */
    @Test
    public void testSize() throws Exception {
        File tempFile = File.createTempFile("SFTPURLTest", ".txt");
        tempFile.deleteOnExit();

        String writtenContents = new Date().toString();
        FileWriter out = new FileWriter(tempFile);
        out.write(writtenContents);
        out.close();

        String path = tempFile.getAbsoluteFile().toURI().getPath();
        URL url = new URL("sftp://" + System.getProperty("user.name") + "@localhost" + path);
        URLConnection conn = url.openConnection();
        assertThat("Size without connect is not 0", conn.getContentLength(), is(-1));
        conn.connect();
        assertThat("Size is not correct", (long) conn.getContentLength(), is(tempFile.length()));
    }

    @Test
    public void testReadDirContents() throws Exception {
        File dir = FileUtil.createTempDir("SFTPURLTest");

        Set<String> filesInDir = new HashSet<String>();
        filesInDir.add("fileA");
        filesInDir.add("fileB");
        for (String s : filesInDir) {
            assertTrue("Temp file " + s + " not created", new File(dir, s).createNewFile());
        }
        filesInDir.add(".");
        filesInDir.add("..");

        String path = dir.getAbsoluteFile().toURI().getPath();
        URL url = new URL("sftp://" + System.getProperty("user.name") + "@localhost" + path + ";type=d");
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        int count = 0;
        String line;
        while ((line = in.readLine()) != null) {
            count++;
            assertTrue("Unexpected file in directory: " + line, filesInDir.contains(line));
        }
        assertThat("Unexpected number of files in directory", count, is(filesInDir.size()));
    }


    @Test
    public void testReadDirContentsNoType() throws Exception {
        File dir = FileUtil.createTempDir("SFTPURLTest");

        String path = dir.getAbsoluteFile().toURI().getPath();
        URL url = new URL("sftp://" + System.getProperty("user.name") + "@localhost" + path);
        expectedEx.expect(IOException.class);
        expectedEx.expectMessage("Cannot read from a directory");
        url.openStream();
    }

    @Test
    public void testReadWithDirType() throws Exception {
        File tempFile = File.createTempFile("SFTPURLTest", ".txt");
        tempFile.deleteOnExit();

        String path = tempFile.getAbsoluteFile().toURI().getPath();
        URL url = new URL("sftp://" + System.getProperty("user.name") + "@localhost" + path + ";type=d");
        expectedEx.expect(IOException.class);
        expectedEx.expectMessage("does not denote a directory");
        url.openStream();
    }


    @Test
    public void testGetHeaderFields() throws Exception {
        File tempFile = File.createTempFile("SFTPURLTest", ".txt");
        tempFile.deleteOnExit();

        String writtenContents = new Date().toString();
        FileWriter out = new FileWriter(tempFile);
        out.write(writtenContents);
        out.close();

        String path = tempFile.getAbsoluteFile().toURI().getPath();
        URL url = new URL("sftp://" + System.getProperty("user.name") + "@localhost" + path);
        URLConnection conn = url.openConnection();
        assertThat("Non-null date received before connect", conn.getHeaderField("date"), is(nullValue()));
        assertThat("Non-null size received before connect", conn.getHeaderField("content-length"), is(nullValue()));
        conn.connect();
        assertThat("Null date received", conn.getHeaderField("date"), is(notNullValue()));
        assertThat("Null size received", conn.getHeaderField("content-length"), is(notNullValue()));

        // content encoding (and some others) are not supported
        assertThat("Non-null content encoding received", conn.getContentEncoding(), is(nullValue()));
    }
}
