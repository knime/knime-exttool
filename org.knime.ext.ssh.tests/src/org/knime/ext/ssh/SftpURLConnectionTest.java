/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
import static org.junit.Assume.assumeNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.knime.core.util.FileUtil;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

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

    private static String host;

    private static String user;

    private static int port;

    private static String keyfilePath;

    @BeforeClass
    public static void setup() {
        strictHostChecking = JSch.getConfig("StrictHostKeyChecking");
        JSch.setConfig("StrictHostKeyChecking", "no");

        String hostString = System.getenv("KNIME_SSHD_ADDRESS");
        assumeNotNull(hostString); // only run on new jenkins
        user = "jenkins";

        final String[] sshdHostInfo = hostString.split(":");

        host = sshdHostInfo[0];
        port = Integer.parseInt(sshdHostInfo[1]);

        final Path sshDir = Paths.get(System.getProperty("user.home"), ".ssh");
        keyfilePath = sshDir.resolve("id_rsa").toString();
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
        final String tempFileName = getTestRoot() + "SftpURLConnectionTestRead" + System.currentTimeMillis() + ".txt";
        final String writtenContents = new Date().toString();

        // write tempfile to remote host
        runCommands("echo '" + writtenContents + "' >> " + tempFileName);

        FileUtil.getWorkflowTempDir(); // dummy call to ensure sftp url is understood

        final URL url = new URL("sftp://" + user + "@" + host + tempFileName);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
            final String readContents = in.readLine();
            assertThat("Unexpected data read via sftp protocol", readContents, is(writtenContents));
        }
    }

    /**
     * Checks if the output stream via the sftp protocol works properly.
     *
     * @throws Exception in an exception occurs
     */
    @Test
    public void testWrite() throws Exception {
        final String tempFile = getTestRoot() + "SftpURLConnectionTestWrite" + System.currentTimeMillis();

        writeAndCheck(tempFile); // test non-existing file
        writeAndCheck(tempFile); // test existing file
    }

    private void writeAndCheck(final String tempFile) throws IOException, JSchException {
        final String writtenContents = new Date().toString();

        FileUtil.getWorkflowTempDir(); // dummy call to ensure sftp url is understood
        final URL url = new URL("sftp://" + user + "@" + host + tempFile);
        final URLConnection conn = url.openConnection();
        try (OutputStream out = conn.getOutputStream()) {
            out.write(writtenContents.getBytes());
            out.flush();
        }

        final String remoteFileContent = runCommands("cat " + tempFile);
        assertThat("Unexpected data written via sftp protocol", remoteFileContent, is(writtenContents));
    }

    @Test
    public void testWriteToDirectory() throws IOException {
        FileUtil.getWorkflowTempDir(); // dummy call to ensure sftp url is understood
        final URL url = new URL("sftp://" + user + "@" + host + getTestRoot());
        final URLConnection conn = url.openConnection();
        expectedEx.expect(IOException.class);
        expectedEx.expectMessage("Cannot write to a directory");
        conn.getOutputStream();
    }

    @Test
    public void testDate() throws Exception {
        final String tempFile = getTestRoot() + "SftpURLConnectionTest_testDate" + System.currentTimeMillis() + ".txt";

        final String output = runCommands("touch " + tempFile + "; date +%s -r " + tempFile).replaceAll("\\s", "");
        final long creationTimeStamp = Integer.parseInt(output);

        FileUtil.getWorkflowTempDir(); // dummy call to ensure sftp url is understood
        final URL url = new URL("sftp://" + user + "@" + host + tempFile);

        final URLConnection conn = url.openConnection();
        assertThat("Modification date without connect is not 0", conn.getDate(), is(0L));
        conn.connect();

        // the date retrieved via sftp only has a resolution of seconds, therefore we eliminate the milliseconds
        assertThat("Modification date retrieved via sftp is wrong", conn.getDate() / 1000, is(creationTimeStamp));
    }

    /**
     * Checks if the input stream via the sftp protocol works properly.
     *
     * @throws Exception in an exception occurs
     */
    @Test
    public void testSize() throws Exception {
        final String tempFile = getTestRoot() + "SftpURLConnectionTest_testSize" + System.currentTimeMillis() + ".txt";

        final String writtenContents = new Date().toString();
        final String result =
            runCommands("echo \"" + writtenContents + "\" >> " + tempFile + ";stat -c %s " + tempFile);
        final long expectedSize = Long.parseLong(result.replaceAll("\\s", "").replaceAll("\\n", ""));

        FileUtil.getWorkflowTempDir(); // dummy call to ensure sftp url is understood

        final URL url = new URL("sftp://" + user + "@" + host + tempFile);
        final URLConnection conn = url.openConnection();
        assertThat("Size without connect is not 0", conn.getContentLength(), is(-1));
        conn.connect();
        assertThat("Size is not correct", (long)conn.getContentLength(), is(expectedSize));
    }

    @Test
    public void testReadDirContents() throws Exception {
        final String dir = getTestRoot() + "SftpURLConnectionsTest_testReadDirContents" + System.currentTimeMillis();

        final String fileA = dir + "/fileA";
        final String fileB = dir + "/fileB";
        runCommands("mkdir " + dir + "; touch " + fileA + "; touch " + fileB);

        final Set<String> filesInDir = new HashSet<>();
        filesInDir.add("fileA");
        filesInDir.add("fileB");
        filesInDir.add(".");
        filesInDir.add("..");

        FileUtil.getWorkflowTempDir(); // dummy call to ensure sftp url is understood

        final URL url = new URL("sftp://" + user + "@" + host + dir + ";type=d");
        try (final BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
            int count = 0;
            String line;
            while ((line = in.readLine()) != null) {
                count++;
                assertTrue("Unexpected file in directory: " + line, filesInDir.contains(line));
            }
            assertThat("Unexpected number of files in directory", count, is(filesInDir.size()));
        }
    }

    @SuppressWarnings("resource")
    @Test
    public void testReadDirContentsNoType() throws Exception {

        final String dir = getTestRoot() + "SftpURLConnectionsTest_testDirContentsNoType" + System.currentTimeMillis();
        runCommands("mkdir " + dir);

        FileUtil.getWorkflowTempDir(); // dummy call to ensure sftp url is understood
        final URL url = new URL("sftp://" + user + "@" + host + dir);
        expectedEx.expect(IOException.class);
        expectedEx.expectMessage("Cannot read from a directory");
        url.openStream();
    }

    @SuppressWarnings("resource")
    @Test
    public void testReadWithDirType() throws Exception {
        final String file = getTestRoot() + "SftpURLConnectionsTest_testReadWithDirType" + System.currentTimeMillis() + ".txt";
        runCommands("touch " + file);

        FileUtil.getWorkflowTempDir(); // dummy call to ensure sftp url is understood
        final URL url = new URL("sftp://" + user + "@" + host + file + ";type=d");
        expectedEx.expect(IOException.class);
        expectedEx.expectMessage("does not denote a directory");
        url.openStream();
    }

    @Test
    public void testGetHeaderFields() throws Exception {

        final String file = getTestRoot() + "SftpURLConnectionsTest_testGetHeaderFields" + System.currentTimeMillis() + ".txt";
        final String writtenContents = new Date().toString();
        runCommands("echo \"" + writtenContents + "\" >> " + file);

        FileUtil.getWorkflowTempDir(); // dummy call to ensure sftp url is understood
        final URL url = new URL("sftp://" + user + "@" + host + file);
        final URLConnection conn = url.openConnection();
        assertThat("Non-null date received before connect", conn.getHeaderField("date"), is(nullValue()));
        assertThat("Non-null size received before connect", conn.getHeaderField("content-length"), is(nullValue()));
        conn.connect();
        assertThat("Null date received", conn.getHeaderField("date"), is(notNullValue()));
        assertThat("Null size received", conn.getHeaderField("content-length"), is(notNullValue()));

        // content encoding (and some others) are not supported
        assertThat("Non-null content encoding received", conn.getContentEncoding(), is(nullValue()));
    }

    /**
     * Run the selected command(s) string on the remote host configured for this test class
     *
     * @param command the command to run
     * @return the output of the command
     * @throws JSchException
     * @throws IOException
     */
    private String runCommands(final String command) throws JSchException, IOException {
        // SSH into remote machine and run the commands
        final JSch j = new JSch();
        j.addIdentity(keyfilePath);

        final Session s = j.getSession(user, host, port);
        s.connect();
        final ChannelExec c = (ChannelExec)s.openChannel("exec");
        c.setCommand(command);

        String result;
        c.setInputStream(null);
        try (InputStream is = c.getInputStream()) {
            c.connect();
            result = IOUtils.toString(is, StandardCharsets.UTF_8);
        }
        c.disconnect();
        s.disconnect();
        return result;
    }

    private static String getTestRoot() {
        String root;
        if ("localhost".equals(host)) {
            // Fix windows paths on localhost
            root = System.getProperty("java.io.tmpdir").replace("C:", "cygdrive/c") + "/";
        } else {
            root = "/tmp/";
        }
        return root;
    }

}
