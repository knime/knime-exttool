/* ------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * History
 *   Jul 14, 2009 (ohl): created
 */
package org.knime.ext.ssh;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.eclipse.jsch.core.IJSchLocation;
import org.eclipse.jsch.core.IJSchService;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.ext.ssh.node.ExtSSHToolSettings;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;
import com.jcraft.jsch.ChannelSftp.LsEntry;

/**
 *
 * @author Peter Ohl, KNIME.com, Zurich, Switzerland
 */
public final class SSHUtil {

    /**
     *
     */
    private SSHUtil() {
        // this class has only static methods
    }

    public static synchronized Session getConnectedSession(
            final ExtSSHToolSettings s) throws Exception {

        int port = s.getPortNumber();
        if (port < 0) {
            port = ExtSSHToolSettings.DEFAULT_SSH_PORT;
        }

        String user = s.getUser();
        if (user == null || user.trim().isEmpty()) {
            user = System.getProperty("user.name");
        }
        String remoteHost = s.getRemoteHost();

        IJSchService service =
                ExtSSHNodeActivator.getDefault().getIJSchService();
        IJSchLocation location = service.getLocation(user, remoteHost, port);
        UserInfo userInfo = s.createJSchUserInfo();

        Session session = null;
        try {
            session = service.createSession(location, userInfo);
            session.connect(s.getTimeoutMilliSec());

            return session;
        } catch (Exception e) {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
            if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                throw e;
            }
            throw new IllegalStateException("Couldn't establish SSH session.",
                    e);
        }
    }

    /**
     * Creates a new ftp channel on a passed session. If the session is null, it
     * creates a new one, according to the settings.
     *
     * @param settings settings (only for timeout if a session is passed).
     * @param session a connected ssh session, or null to also create a new
     *            session.
     * @return a new sftp channel
     * @throws Exception if things go wrong
     */
    public static ChannelSftp createNewSFTPChannel(
            final ExtSSHToolSettings settings, final Session session)
            throws Exception {

        ChannelSftp ftpChannel = null;
        Session s = session;
        if (s == null) {
            s = getConnectedSession(settings);
        }

        try {
            ftpChannel = (ChannelSftp)s.openChannel("sftp");
            ftpChannel.connect(settings.getTimeoutMilliSec());

        } catch (Exception t) {
            if (session == null) {
                // if we created a session just for this channel
                if (s.isConnected()) {
                    s.disconnect();
                }
            }
            throw t;
        }

        return ftpChannel;
    }

    /**
     * @param ftpChannel
     * @param src
     * @param dest
     * @param exec
     * @throws SftpException
     * @throws CanceledExecutionException
     */
    public static void ftpPutRec(final ChannelSftp ftpChannel, final File src,
            final String dest, final ExecutionMonitor exec)
            throws SftpException, CanceledExecutionException {
        exec.checkCanceled();

        String destPath = linuxPath(dest);

        if (src.isFile()) {
            ftpChannel.put(src.getAbsolutePath(), destPath);
            return;
        }
        if (src.isDirectory()) {
            ftpMkdirs(ftpChannel, destPath);
        }

        File[] files = src.listFiles();
        // if srcFiles is not readable this could be null
        if (files != null) {
            for (File f : files) {
                ftpPutRec(ftpChannel, f, destPath + "/" + f.getName(), exec);
            }
        }
    }

    /**
     * Recursively fetches files and dirs.
     *
     * @param ftpChannel open ftp conenction
     * @param src the remote file or dir to get
     * @param dest the local destination
     * @param exec
     * @throws SftpException boom.
     * @throws IOException buhm.
     * @throws CanceledExecutionException
     */
    public static void ftpGetRec(final ChannelSftp ftpChannel,
            final String src, final File dest, final ExecutionMonitor exec)
            throws SftpException, IOException, CanceledExecutionException {

        exec.checkCanceled();

        String srcPath = linuxPath(src);

        // see if the src is a directory
        try {
            ftpChannel.cd(srcPath);
        } catch (SftpException e) {
            // then treat it like a file
            ftpChannel.get(srcPath, dest.getAbsolutePath());
            return;
        }

        // here src is a remote directory
        if (dest.exists()) {
            if (!dest.isDirectory()) {
                throw new IOException("FTP source is a directory, but "
                        + "local destination is not.");
            }
        } else {
            // try creating the local dir
            if (!dest.mkdirs()) {
                throw new IOException("Unable to create local destination "
                        + "directory" + dest.getAbsolutePath());
            }
        }
        Vector<LsEntry> files = ftpChannel.ls(srcPath);
        for (LsEntry e : files) {
            if (e.getFilename().equals(".")) {
                continue;
            }
            if (e.getFilename().equals("..")) {
                continue;
            }
            ftpGetRec(ftpChannel, srcPath + "/" + e.getFilename(),
                    new File(dest, e.getFilename()), exec);
        }

    }

    /**
     * @param ftpChannel
     * @param remoteDir
     * @throws SftpException
     */
    public static void ftpMkdirs(final ChannelSftp ftpChannel,
            final String remoteDir) throws SftpException {

        List<String> rmtSegm = splitPath(remoteDir);
        String rmtPath = linuxPath(rmtSegm);

        try {
            // see if it already exists
            SftpATTRS attr = ftpChannel.lstat(rmtPath);
            if (!attr.isDir()) {
                throw new SftpException(0,
                        "Remote entry exists, but is not a directory");
            }
        } catch (SftpException e) {
            try {
                // try creating it - works only if the parent exists
                ftpChannel.mkdir(rmtPath);
            } catch (SftpException s) {
                // try creating the parent dir then
                if (rmtSegm.size() > 1) {
                    rmtSegm.remove(0);
                    ftpMkdirs(ftpChannel, linuxPath(rmtSegm));
                    ftpChannel.mkdir(rmtPath);
                    return;
                } else {
                    throw new SftpException(0,
                            "Couldn't create remote directory");
                }
            }
        }
    }

    /**
     * Splits a file/dir path into its segments.
     *
     * @return a list of path segments. If the argument denotes a file the first
     *         list element (index 0) is the file name - all others are the
     *         directories in the file's path. The highest index always contains
     *         the directory located in the root of the file system (or the
     *         drive letter (e. g. "C:") in some OS).
     */
    public static List<String> splitPath(final String filePath) {
        if (filePath == null) {
            return null;
        }
        LinkedList<String> segments = new LinkedList<String>();

        String p = filePath;
        while (p != null && !p.isEmpty() && !p.equals("\\") && !p.equals("/")) {
            File f = new File(p);
            segments.add(f.getName());
            p = f.getParent();
        }
        return segments;
    }

    public static String linuxPath(final List<String> segments) {
        if (segments == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (int i = segments.size() - 1; i >= 0; i--) {
            result.append("/").append(segments.get(i));
        }
        return result.toString();
    }

    public static String linuxPath(final String anyPath) {
        return linuxPath(splitPath(anyPath));

    }

    /**
     * Deletes the remote file or directory. Directories are deleted with their
     * content.
     *
     * @param ftpChannel a connected ftp channel
     * @param remoteFile the remote file to delete
     * @throws SftpException
     */
    public static void ftpDelRec(final ChannelSftp ftpChannel,
            final String remoteFile) throws SftpException {
        String rmtPath = linuxPath(remoteFile);
        SftpATTRS attr = ftpChannel.lstat(rmtPath);
        if (attr.isDir()) {
            Vector<LsEntry> files = ftpChannel.ls(rmtPath);
            for (LsEntry e : files) {
                if (e.getFilename().equals(".")) {
                    continue;
                }
                if (e.getFilename().equals("..")) {
                    continue;
                }
                ftpDelRec(ftpChannel, rmtPath + "/" + e.getFilename());
            }
            ftpChannel.rmdir(rmtPath);
        } else {
            ftpChannel.rm(rmtPath);
        }
    }

}
