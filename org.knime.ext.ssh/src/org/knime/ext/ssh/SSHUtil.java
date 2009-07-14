/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 *
 * History
 *   Jul 14, 2009 (ohl): created
 */
package org.knime.ext.ssh;

import java.io.File;
import java.io.IOException;
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

        File dstFile = new File(dest);

        if (src.isFile()) {
            ftpChannel.put(src.getAbsolutePath(), dest);
            return;
        }
        if (src.isDirectory()) {
            ftpMkdirs(ftpChannel, dest);
        }

        File[] files = src.listFiles();
        // if srcFiles is not readable this could be null
        if (files != null) {
            for (File f : files) {
                String d = new File(dstFile, f.getName()).getAbsolutePath();
                ftpPutRec(ftpChannel, f, d, exec);
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

        // see if the src is a directory
        try {
            ftpChannel.cd(src);
        } catch (SftpException e) {
            // then treat it like a file
            ftpChannel.get(src, dest.getAbsolutePath());
            return;
        }

        // here src is a remote directory
        // we just cd'ed into it (in the try block)
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
        Vector<LsEntry> files = ftpChannel.ls(src);
        for (LsEntry e : files) {
            if (e.getFilename().equals(".")) {
                continue;
            }
            if (e.getFilename().equals("..")) {
                continue;
            }
            ftpGetRec(ftpChannel, new File(src, e.getFilename())
                    .getAbsolutePath(), new File(dest, e.getFilename()), exec);
        }

    }

    /**
     * @param ftpChannel
     * @param remoteDir
     * @throws SftpException
     */
    public static void ftpMkdirs(final ChannelSftp ftpChannel,
            final String remoteDir) throws SftpException {
        try {
            // see if it already exists
            SftpATTRS attr = ftpChannel.lstat(remoteDir);
            if (!attr.isDir()) {
                throw new SftpException(0,
                        "Remote entry exists, but is not a directory");
            }
        } catch (SftpException e) {
            try {
                // try creating it - works only if the parent exists
                ftpChannel.mkdir(remoteDir);
            } catch (SftpException s) {
                File d = new File(remoteDir);
                if (d.getParentFile() != null) {
                    // try creating the parent dir then
                    ftpMkdirs(ftpChannel, d.getParentFile().getAbsolutePath());
                    ftpChannel.mkdir(remoteDir);
                    return;
                } else {
                    throw new SftpException(0,
                            "Couldn't create remote directory");
                }
            }
        }
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
        SftpATTRS attr = ftpChannel.lstat(remoteFile);
        if (attr.isDir()) {
            Vector<LsEntry> files = ftpChannel.ls(remoteFile);
            for (LsEntry e : files) {
                if (e.getFilename().equals(".")) {
                    continue;
                }
                if (e.getFilename().equals("..")) {
                    continue;
                }
                ftpDelRec(ftpChannel, new File(remoteFile, e.getFilename())
                        .getAbsolutePath());
            }
            ftpChannel.rmdir(remoteFile);
        } else {
            ftpChannel.rm(remoteFile);
        }
    }

}
