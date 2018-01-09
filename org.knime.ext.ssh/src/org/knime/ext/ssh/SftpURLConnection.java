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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jsch.core.IJSchService;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

/**
 * {@link URLConnection} that can read and write via sftp. The expected URL format is the
 * following:
 * <div>
 * <tt>sftp://</tt><i>[user[:password]</i><tt>@</tt><i>]hostname</i><tt>/</tt><i>path[</i><tt>;type=d</tt><i>]</i>
 * </div>
 * If <tt>;type=d</tt> is specified at the end of the URL and the path denotes a directory on the remote computer,
 * the contents of the directory will be read, each in one line.
 *
 *
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class SftpURLConnection extends URLConnection {
    private static final Map<String, SoftReference<Session>> sessionCache =
        new HashMap<String, SoftReference<Session>>();

    /** Protocol scheme for SFTP connection: {@value} . */
    public static final String SCHEME = "sftp";

    private Session m_session;

    private SftpATTRS m_attrs;

    private static final Pattern TYPE_PATTERN = Pattern.compile("^(.+);type=([aid])$");

    /**
     * Creates a new connection via SFTP.
     *
     * @param u an URL with sftp scheme
     */
    SftpURLConnection(final URL u) {
        super(u);
    }

    private String getPath() {
        Matcher m = TYPE_PATTERN.matcher(url.getPath());
        if (m.matches()) {
            return m.group(1);
        } else {
            return url.getPath();
        }
    }

    private boolean dirContentsRequested() {
        Matcher m = TYPE_PATTERN.matcher(url.getPath());
        return m.matches() && "d".equals(m.group(2));
    }

    private Session getSession(final boolean forceNewSession) throws JSchException {
        String key = url.getUserInfo() + "@" + url.getHost();
        SoftReference<Session> ref = sessionCache.get(key);
        Session s;

        if ((ref != null) && forceNewSession) {
            if ((s = ref.get()) != null) {
                s.disconnect();
            }
            sessionCache.remove(key);
            ref = null;
        }

        if ((ref == null) || ((s = ref.get()) == null) || !s.isConnected()) {
            IJSchService service = ExtSSHNodeActivator.getDefault().getIJSchService();
            String userInfo = url.getUserInfo();
            String username, password;
            if (userInfo != null) {
                int colonIndex = userInfo.indexOf(':');
                if (colonIndex >= 0) {
                    username = userInfo.substring(0, colonIndex);
                    password = userInfo.substring(colonIndex + 1);
                } else {
                    username = userInfo;
                    password = null;
                }
            } else {
                username = System.getProperty("user.name");
                password = null;
            }

            s = service.createSession(url.getHost(), url.getPort(), username);
            s.setPassword(password);
            s.setTimeout(getReadTimeout());
            s.setHostKeyRepository(service.getJSch().getHostKeyRepository());
            s.connect(getConnectTimeout());
            sessionCache.put(key, new SoftReference<Session>(s));
        }
        return s;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connect() throws IOException {
        ChannelSftp channel = null;
        try {
            channel = openChannel();
            m_attrs = channel.lstat(getPath());
            connected = true;
        } catch (JSchException ex) {
            throw new IOException(ex);
        } catch (SftpException ex) {
            if (!ex.getMessage().equals("No such file")) {
                throw new IOException(ex);
            }
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private ChannelSftp openChannel() throws JSchException {
        // the retry is a workaround for when there are too many open connections in one session
        int retries = 1;
        JSchException lastException = null;
        do {
            m_session = getSession(retries == 0);
            ChannelSftp channel = (ChannelSftp)m_session.openChannel("sftp");
            try {
                channel.connect(getConnectTimeout());
                return channel;
            } catch (JSchException ex) {
                lastException = ex;
                if (m_session.isConnected() && (channel.getExitStatus() != 1)) {
                    throw ex;
                } // otherwise try again, e.g. if session is down
            }
        } while (retries-- > 0);
        throw lastException;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHeaderField(final String name) {
        if (m_attrs == null) {
            return super.getHeaderField(name);
        } else if ("date".equals(name)) {
            return m_attrs.getMtimeString();
        } else if ("content-length".equals(name)) {
            return Long.toString(m_attrs.getSize());
        } else {
            return super.getHeaderField(name);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDate() {
        if (m_attrs == null) {
            return super.getDate();
        } else {
            return m_attrs.getMTime() * 1000L;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getInputStream() throws IOException {
        if (!connected) {
            connect();
        }
        try {
            if ((m_attrs != null) && m_attrs.isDir()) {
                if (dirContentsRequested()) {
                    return createDirContentsInputStream();
                } else {
                    throw new IOException("Cannot read from a directory. Append \";type=d\" to the URL if you want "
                            + "to read directory contents.");
                }
            } else {
                if (dirContentsRequested()) {
                    throw new IOException("Directory contents requested, but URL does not denote a directory");
                } else {
                    return createFileInputStream();
                }
            }
        } catch (SftpException ex) {
            throw new IOException(ex);
        } catch (JSchException ex) {
            throw new IOException(ex);
        }
    }

    private InputStream createFileInputStream() throws JSchException, SftpException, UnsupportedEncodingException {
        final ChannelSftp channel = openChannel();
        final InputStream in = channel.get(URLDecoder.decode(url.getPath(), "UTF-8"));

        return new InputStream() {
            /**
             * {@inheritDoc}
             */
            @Override
            public int available() throws IOException {
                return in.available();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void close() throws IOException {
                in.close();
                channel.disconnect();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public synchronized void mark(final int readlimit) {
                in.mark(readlimit);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean markSupported() {
                return in.markSupported();
            }

            @Override
            public int read() throws IOException {
                return in.read();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public int read(final byte[] b) throws IOException {
                return in.read(b);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public int read(final byte[] b, final int off, final int len) throws IOException {
                return in.read(b, off, len);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public synchronized void reset() throws IOException {
                in.reset();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public long skip(final long n) throws IOException {
                return in.skip(n);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream getOutputStream() throws IOException {
        if (!connected) {
            connect();
        }
        if ((m_attrs != null) && m_attrs.isDir()) {
            throw new IOException("Cannot write to a directory");
        }
        try {
            return createFileOutputStream();
        } catch (SftpException ex) {
            throw new IOException(ex);
        } catch (JSchException ex) {
            throw new IOException(ex);
        }
    }

    private OutputStream createFileOutputStream() throws SftpException, JSchException, UnsupportedEncodingException {
        final ChannelSftp channel = openChannel();
        final OutputStream out = channel.put(URLDecoder.decode(url.getPath(), "UTF-8"));

        return new OutputStream() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void close() throws IOException {
                out.close();
                channel.disconnect();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public void write(final int b) throws IOException {
                out.write(b);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void write(final byte[] b) throws IOException {
                out.write(b);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void write(final byte[] b, final int off, final int len) throws IOException {
                out.write(b, off, len);
            }
        };

    }

    private InputStream createDirContentsInputStream() throws SftpException, JSchException {
        final ChannelSftp channel = (ChannelSftp)m_session.openChannel("sftp");
        channel.connect(getConnectTimeout());
        @SuppressWarnings("unchecked")
        Vector<ChannelSftp.LsEntry> entries = channel.ls(getPath());
        channel.disconnect();
        StringBuilder buf = new StringBuilder();
        for (LsEntry e : entries) {
            buf.append(e.getFilename()).append('\n');
        }

        return new ByteArrayInputStream(buf.toString().getBytes(Charset.forName("UTF-8")));
    }
}
