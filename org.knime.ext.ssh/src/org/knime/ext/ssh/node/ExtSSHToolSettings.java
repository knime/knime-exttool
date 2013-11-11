/* ------------------------------------------------------------------
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
 * History
 *   14.10.2008 (ohl): created
 */
package org.knime.ext.ssh.node;

import java.io.File;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.KnimeEncryption;

import com.jcraft.jsch.UserInfo;

/**
 *
 * @author ohl, University of Konstanz
 */
public class ExtSSHToolSettings {

    // Windows accepts windows and unix paths, unix only accepts unix paths
    private static final String VALID_PATH_REGEX = File.separatorChar == '/' ? "/.*" : "[\\\\/].*|[a-zA-Z]:\\\\.*";

    /** default port. */
    public static final int DEFAULT_SSH_PORT = 22;

    private static final String CFG_HOST = "remoteHost";

    private static final String CFG_PORT = "sshPortNumber";

    private static final String CFG_USER = "userName";

    private static final String CFG_PASSWD = "passwd";

    private static final String CFG_KEY_PASSPHRASE = "keyPassphrase";

    private static final String CFG_TIMEOUT = "TimeoutSec";

    private static final String CFG_COMMAND = "remoteCommand";

    private static final String CFG_REMOTEINPUT = "remoteInputFile";

    private static final String CFG_REMOTEOUTPUT = "remoteOutputFile";

    private static final String CFG_DISABLE_KNOWN_HOSTS = "disableKnownHosts";

    private String m_remoteHost;

    private int m_portNumber;

    private String m_user;

    private String m_encryptPassword;

    private String m_encryptKeyPassphrase;

    private int m_timeout;

    private String m_command;

    private String m_remoteInputFile;

    private String m_remoteOutputFile;

    private boolean m_disableKnownHosts;

    /**
     * Default constructor with default settings, possibly invalid settings.
     */
    public ExtSSHToolSettings() {
        m_remoteHost = "";
        m_user = "";
        m_encryptKeyPassphrase = "";
        m_encryptPassword = "";
        m_timeout = 0;
        m_portNumber = DEFAULT_SSH_PORT;
        m_command = "";
        m_remoteInputFile = "";
        m_remoteOutputFile = "";
        m_disableKnownHosts = false;
    }

    /**
     * Creates a new settings object with values from the object passed.
     *
     * @param settings object with the new values to set
     * @throws InvalidSettingsException if settings object is invalid
     */
    public ExtSSHToolSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_remoteHost = settings.getString(CFG_HOST);
        m_portNumber = settings.getInt(CFG_PORT);
        m_timeout = settings.getInt(CFG_TIMEOUT);
        m_user = settings.getString(CFG_USER);
        m_encryptPassword = settings.getString(CFG_PASSWD);
        m_encryptKeyPassphrase = settings.getString(CFG_KEY_PASSPHRASE);
        m_command = settings.getString(CFG_COMMAND);
        m_remoteInputFile = settings.getString(CFG_REMOTEINPUT);
        m_remoteOutputFile = settings.getString(CFG_REMOTEOUTPUT);
        m_disableKnownHosts = settings.getBoolean(CFG_DISABLE_KNOWN_HOSTS, false);
    }

    /**
     * Create the JSch User info object that returns password and passphrase.
     *
     * @return a new user info object.
     */
    public UserInfo createJSchUserInfo() {
        return new SettingsUserInfo();
    }

    /**
     * Saves the current values in to the settings object.
     *
     * @param settings the config object to store values in
     */
    public void save(final NodeSettingsWO settings) {
        settings.addString(CFG_HOST, m_remoteHost);
        settings.addInt(CFG_PORT, m_portNumber);
        settings.addInt(CFG_TIMEOUT, m_timeout);
        settings.addString(CFG_USER, m_user);
        settings.addString(CFG_PASSWD, m_encryptPassword);
        settings.addString(CFG_KEY_PASSPHRASE, m_encryptKeyPassphrase);
        settings.addString(CFG_COMMAND, m_command);
        settings.addString(CFG_REMOTEINPUT, m_remoteInputFile);
        settings.addString(CFG_REMOTEOUTPUT, m_remoteOutputFile);
        settings.addBoolean(CFG_DISABLE_KNOWN_HOSTS, m_disableKnownHosts);
    }

    /**
     * Returns null if the settings are valid. Otherwise a user message telling
     * which settings are incorrect and why.
     *
     * @return an error message if settings are invalid, of null, if everything
     *         is alright
     */
    public String getStatusMsg() {
        if (m_remoteHost.isEmpty()) {
            return "The remote host must be specified";
        }
        if (m_command.isEmpty()) {
            return "The remote command must be specified.";
        }
        if (m_remoteInputFile.isEmpty()) {
            return "The remote path for the input table file must be set.";
        }
        if (m_remoteOutputFile.isEmpty()) {
            return "The remote path for the output table file must be set.";
        }
        if (!m_remoteInputFile.matches(VALID_PATH_REGEX)) {
            return "'" + m_remoteInputFile + "' is not a valid absolute path for the remote input file.";
        }
        if (!m_remoteOutputFile.matches(VALID_PATH_REGEX)) {
            return "'" + m_remoteOutputFile + "' is not a valid absolute path for the remote output file.";
        }
        return null;
    }

    /**
     * @param remoteHost the remoteHost to set
     */
    public void setRemoteHost(final String remoteHost) {
        if (remoteHost == null) {
            m_remoteHost = "";
        } else {
            m_remoteHost = remoteHost;
        }
    }

    /**
     * @return the remoteHost
     */
    public String getRemoteHost() {
        return m_remoteHost;
    }

    /**
     * @param user the user to set
     */
    public void setUser(final String user) {
        if (user == null) {
            m_user = "";
        } else {
            m_user = user;
        }
    }

    /**
     * @return the user
     */
    public String getUser() {
        return m_user;
    }

    /**
     * Gets the specified username with the $user§ and $userhome$ reference
     * replaced.
     *
     * @return the username with $user$ or $userhome$ replaced
     */
    public String getUserResolved() {
        return replaceUserVariable(m_user);
    }

    /**
     *
     * @return the encrypted password
     */
    public String getEncryptPassword() {
        return m_encryptPassword;
    }

    /**
     *
     * @param newEncrPasswd the new encrypted password
     */
    public void setEncryptPassword(final String newEncrPasswd) {
        m_encryptPassword = newEncrPasswd;
    }

    /**
     * @return the encryptedKeyPassphrase
     */
    public String getEncryptKeyPassphrase() {
        return m_encryptKeyPassphrase;
    }

    /**
     * @param encryptedKeyPassphrase the encryptedKeyPassphrase to set
     */
    public void setEncryptKeyPassphrase(final String encryptedKeyPassphrase) {
        m_encryptKeyPassphrase = encryptedKeyPassphrase;
    }

    /**
     * @param portNumber the portNumber to set
     */
    public void setPortNumber(final int portNumber) {
        m_portNumber = portNumber;
    }

    /**
     * @return the portNumber
     */
    public int getPortNumber() {
        return m_portNumber;
    }

    /**
     * @param timeout the timeout to set in seconds
     */
    public void setTimeout(final int timeout) {
        m_timeout = timeout;
    }

    /**
     * @return the timeout in seconds
     */
    public int getTimeout() {
        return m_timeout;
    }

    /**
     *
     * @return the timeout in milliseconds (or 0 if none is set or not
     *         representable in an int)
     */
    public int getTimeoutMilliSec() {
        int timeoutSec = getTimeout();
        int timeoutMilliSec = 0; // 0 is no timeout
        if (timeoutSec > 0 && timeoutSec < Integer.MAX_VALUE / 1000) {
            timeoutMilliSec = timeoutSec * 1000;
        }
        return timeoutMilliSec;
    }

    /**
     * @return the command
     */
    public String getCommand() {
        return m_command;
    }

    /**
     * @param command the command to set
     */
    public void setCommand(final String command) {
        if (command == null) {
            m_command = "";
        } else {
            m_command = command;
        }
    }

    /**
     * @return the remoteInputFile
     */
    public String getRemoteInputFile() {
        return m_remoteInputFile;
    }

    /**
     * @param remoteInputFile the remoteInputFile to set
     */
    public void setRemoteInputFile(final String remoteInputFile) {
        m_remoteInputFile = remoteInputFile;
    }

    /**
     * @return the remoteOutputFile
     */
    public String getRemoteOutputFile() {
        return m_remoteOutputFile;
    }

    /**
     * @param remoteOutputFile the remoteOutputFile to set
     */
    public void setRemoteOutputFile(final String remoteOutputFile) {
        m_remoteOutputFile = remoteOutputFile;
    }

    /**
     * @return the disableKnownHosts
     */
    public boolean getDisableKnownHosts() {
        return m_disableKnownHosts;
    }

    /**
     * @param disableKnownHosts the disableKnownHosts to set
     */
    public void setDisableKnownHosts(final boolean disableKnownHosts) {
        m_disableKnownHosts = disableKnownHosts;
    }

    /**
     * pattern in the string that will be replaced by the current user name (see
     * {@link #replaceUserVariable(String)}).
     */
    public static final String USER_NAME_PATTERN = "$user$";

    /**
     * Pattern in the string that will be replaced by the current user home
     * directory (see {@link #replaceUserVariable(String)}).
     */
    public static final String USER_HOME_PATTERN = "$userhome$";

    /**
     * both patterns must start with the same character and the one must not be
     * a prefix of the other.
     */
    private static final char FIRST_CHAR = USER_HOME_PATTERN.charAt(0);

    /**
     *
     * @param value the string to replace the patterns in.
     * @return either the parameter or a new string with the pattern replaced by
     *         the current user
     */
    public static String replaceUserVariable(final String value) {

        if (value == null || value.isEmpty()) {
            return value;
        }

        // this must use the shortest pattern(!!!)
        int lastPosIdx = value.length() - USER_NAME_PATTERN.length();
        if (lastPosIdx < 0) {
            // the shortest pattern doesn't fit in the value
            return value;
        }

        StringBuilder result = new StringBuilder();
        int lastAddedIdx = -1;
        int firstCharIdx = -1;
        while (true) {
            firstCharIdx = value.indexOf(FIRST_CHAR, firstCharIdx + 1);
            if (firstCharIdx < 0 || firstCharIdx > lastPosIdx) {
                break;
            }

            // check user home
            if (firstCharIdx + USER_HOME_PATTERN.length() <= value.length()
                    && value.substring(firstCharIdx,
                            firstCharIdx + USER_HOME_PATTERN.length()).equals(
                            USER_HOME_PATTERN)) {
                // append everything up to the pattern
                result.append(value.substring(lastAddedIdx + 1, firstCharIdx));
                // add the replaced pattern
                result.append(System.getProperty("user.home"));
                lastAddedIdx = firstCharIdx + USER_HOME_PATTERN.length() - 1;
                firstCharIdx += USER_HOME_PATTERN.length() - 1;
                // check username (its the shortest pattern - it must fit)
            } else if (value.substring(firstCharIdx,
                    firstCharIdx + USER_NAME_PATTERN.length()).equals(
                    USER_NAME_PATTERN)) {
                // append everything up to the pattern
                result.append(value.substring(lastAddedIdx + 1, firstCharIdx));
                // add the replaced pattern
                result.append(System.getProperty("user.name"));
                lastAddedIdx = firstCharIdx + USER_NAME_PATTERN.length() - 1;
                firstCharIdx += USER_NAME_PATTERN.length() - 1;
            }
        }

        if (lastAddedIdx == -1) {
            // nothing added to the result - because we didn't find any pattern
            return value;
        } else {
            result.append(value.substring(lastAddedIdx + 1));
        }

        return result.toString();
    }

    /**
     * pattern in the string that will be replaced by the value of the remote
     * input file in the settings.
     */
    public static final String IN_FILE_PATTERN = "$inFile";

    /**
     * pattern in the string that will be replaced by the value of the remote
     * input file in the settings.
     */
    public static final String OUT_FILE_PATTERN = "$outFile";

    /**
     * Replaces occurrences of the IN_FILE_PATTERN and the OUT_FILE_PATTERN in
     * the cmd string.
     *
     * @param cmd the string in which to place all occurrences of the patterns
     * @param settings contains the value to replace the patterns with
     * @return the string with the replacements
     */
    public static String replaceTempFileVariables(final String cmd,
            final ExtSSHToolSettings settings) {
        String result =
                cmd.replace(IN_FILE_PATTERN, settings.getRemoteInputFile());
        result =
                result
                        .replace(OUT_FILE_PATTERN, settings
                                .getRemoteOutputFile());
        return result;
    }

    private class SettingsUserInfo implements UserInfo {

        /** {@inheritDoc} */
        @Override
        public String getPassphrase() {
            String encryptPassphrase =
                    ExtSSHToolSettings.this.getEncryptKeyPassphrase();
            if (encryptPassphrase == null || encryptPassphrase.length() == 0) {
                return null;
            }
            try {
                return KnimeEncryption.decrypt(encryptPassphrase);
            } catch (Exception e) {
                throw new RuntimeException("Unable to decrypt password: "
                        + e.getMessage(), e);
            }
        }

        /** {@inheritDoc} */
        @Override
        public String getPassword() {
            String encryptPasswd = ExtSSHToolSettings.this.getEncryptPassword();
            if (encryptPasswd == null || encryptPasswd.length() == 0) {
                return null;
            }
            try {
                return KnimeEncryption.decrypt(encryptPasswd);
            } catch (Exception e) {
                throw new RuntimeException("Unable to decrypt password: "
                        + e.getMessage(), e);
            }
        }

        /** {@inheritDoc} */
        @Override
        public boolean promptPassphrase(final String arg0) {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public boolean promptPassword(final String arg0) {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public boolean promptYesNo(final String arg0) {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public void showMessage(final String arg0) {
            // empty
        }

    }

}
