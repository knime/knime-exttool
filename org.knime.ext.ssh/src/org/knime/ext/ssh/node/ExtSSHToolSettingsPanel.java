/* ------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   05.06.2009 (ohl): created
 */
package org.knime.ext.ssh.node;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.KnimeEncryption;
import org.knime.ext.ssh.SSHUtil;

/**
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class ExtSSHToolSettingsPanel extends JPanel {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(ExtSSHToolSettingsPanel.class);

    private static final long serialVersionUID = 1L;

    private final JTextField m_host = new JTextField(30);

    private final JTextField m_port = new JTextField(5);

    private final JTextField m_timeout = new JTextField(5);

    private final JTextField m_user = new JTextField(30);

    private final JPasswordField m_password = new JPasswordField(30);

    private boolean m_passwordChanged = false;

    private final JPasswordField m_keyPassphrase = new JPasswordField(30);

    private boolean m_keyPassphraseChanged = false;

    private final JTextField m_command = new JTextField(50);

    private final JTextField m_remoteInFile = new JTextField(50);

    private final JTextField m_remoteOutFile = new JTextField(50);

    /**
     * Creates a new tab.
     */
    ExtSSHToolSettingsPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        /*
         * Bordered box for connection controls
         */
        Box connectBox = Box.createVerticalBox();
        connectBox.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Connect Info:"));

        // Host + Port + Timeout
        Box hostBox = Box.createHorizontalBox();
        hostBox.add(new JLabel("Host:"));
        hostBox.add(Box.createHorizontalStrut(3));
        hostBox.add(m_host);
        m_host.setToolTipText("Enter hostname or IP address");
        hostBox.add(Box.createHorizontalStrut(15));
        hostBox.add(new JLabel("Port:"));
        hostBox.add(Box.createHorizontalStrut(3));
        hostBox.add(m_port);
        m_port.setToolTipText("Leave empty for default port #"
                + ExtSSHToolSettings.DEFAULT_SSH_PORT);
        hostBox.add(Box.createHorizontalStrut(15));
        hostBox.add(new JLabel("Timeout (sec):"));
        hostBox.add(Box.createHorizontalStrut(3));
        hostBox.add(m_timeout);
        hostBox.add(Box.createHorizontalGlue());

        connectBox.add(Box.createVerticalStrut(5));
        connectBox.add(hostBox);

        // User + password
        Box userBox = Box.createHorizontalBox();
        userBox.add(new JLabel("Username:"));
        userBox.add(Box.createHorizontalStrut(3));
        userBox.add(m_user);
        m_user.setToolTipText("Leave empty for current user");
        userBox.add(Box.createHorizontalStrut(15));
        userBox.add(new JLabel("Password:"));
        userBox.add(Box.createHorizontalStrut(3));
        userBox.add(m_password);
        m_password.addFocusListener(new FocusAdapter() {
            /** {@inheritDoc} */
            @Override
            public void focusGained(final FocusEvent e) {
                m_password.setText("");
                m_passwordChanged = true;
            }
        });
        m_password.setToolTipText("Leave empty if not needed");
        userBox.add(Box.createHorizontalGlue());

        connectBox.add(Box.createVerticalStrut(5));
        connectBox.add(userBox);

        // passphrase + check button
        Box phraseBox = Box.createHorizontalBox();
        phraseBox.add(new JLabel("Passphrase for private key file:"));
        phraseBox.add(Box.createHorizontalStrut(3));
        phraseBox.add(m_keyPassphrase);
        m_keyPassphrase.addFocusListener(new FocusAdapter() {
            /** {@inheritDoc} */
            @Override
            public void focusGained(final FocusEvent e) {
                m_keyPassphrase.setText("");
                m_keyPassphraseChanged = true;
            }
        });

        m_keyPassphrase.setToolTipText("Leave empty if not used or required");
        phraseBox.add(Box.createHorizontalGlue());

        phraseBox.add(Box.createHorizontalGlue());
        phraseBox.add(Box.createHorizontalGlue());

        JButton checkButton = new JButton("Check Connection");
        checkButton.setPreferredSize(new Dimension(120, 25));
        checkButton.setMaximumSize(new Dimension(120, 25));
        checkButton.setMinimumSize(new Dimension(120, 25));
        checkButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                checkConnection();
            }
        });
        phraseBox.add(checkButton);

        connectBox.add(Box.createVerticalStrut(5));
        connectBox.add(phraseBox);

        connectBox.add(Box.createVerticalStrut(8));
        connectBox.add(new JLabel("Please make sure the hostname appears in "
                + "the list of known hosts in the \"SSH2\" preference page"));
        /*
         * Bordered box containing all remote command controls
         */
        Box commandBox = Box.createVerticalBox();
        commandBox.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Remote Command:"));

        // command line
        Box cmdBox = Box.createHorizontalBox();
        cmdBox.add(new JLabel("Remote command line:"));
        cmdBox.add(Box.createHorizontalStrut(3));
        cmdBox.add(m_command);
        m_command.setToolTipText("Command plus arguments. $inFile and "
                + "$outFile is replaced by the corresp. file name");
        cmdBox.add(Box.createHorizontalGlue());

        commandBox.add(Box.createVerticalStrut(5));
        commandBox.add(cmdBox);

        // remote temp input file
        Box inFileBox = Box.createHorizontalBox();
        inFileBox.add(new JLabel("Temp. remote input filename:"));
        inFileBox.add(Box.createHorizontalStrut(3));
        inFileBox.add(m_remoteInFile);
        m_remoteInFile.setToolTipText("Input data is written into this file. "
                + "Enter full remote path and filename.");
        inFileBox.add(Box.createHorizontalGlue());

        commandBox.add(Box.createVerticalStrut(5));
        commandBox.add(inFileBox);

        // remote temp output file
        Box outFileBox = Box.createHorizontalBox();
        outFileBox.add(new JLabel("Temp. remote output filename:"));
        outFileBox.add(Box.createHorizontalStrut(3));
        outFileBox.add(m_remoteOutFile);
        m_remoteOutFile.setToolTipText("Output data must be in this file. "
                + "Enter full remote path and filename.");
        outFileBox.add(Box.createHorizontalGlue());

        commandBox.add(Box.createVerticalStrut(5));
        commandBox.add(outFileBox);

        // create the panel
        add(Box.createVerticalStrut(5));
        add(connectBox);
        add(Box.createVerticalStrut(10));
        add(commandBox);

    }

    /**
     * Called by the parent to load new settings into the tab.
     *
     * @param settings the new settings to take over
     */
    void loadSettings(final ExtSSHToolSettings settings) {
        transferSettingsIntoComponents(settings);
    }

    /**
     * Called by the parent to get current values saved into the settings
     * object.
     *
     * @param settings the object to write the currently entered values into
     */
    void saveSettings(final ExtSSHToolSettings settings)
            throws InvalidSettingsException {
        transferComponentsValuesIntoSettings(settings);
    }

    /**
     * Transfers the currently entered values from this tab's components into
     * the provided settings object.
     */
    private void transferComponentsValuesIntoSettings(
            final ExtSSHToolSettings settings) throws InvalidSettingsException {

        settings.setRemoteHost(m_host.getText().trim());

        try {
            String portnumber = m_port.getText().trim();
            if (!portnumber.isEmpty()) {
                int portNr = Integer.parseInt(portnumber);
                settings.setPortNumber(portNr);
            } else {
                settings.setPortNumber(-1);
            }
        } catch (NumberFormatException nfe) {
            throw new InvalidSettingsException(
                    "Invalid port number (please enter a number).");
        }

        try {
            String timeout = m_timeout.getText().trim();
            if (!timeout.isEmpty()) {
                int t = Integer.parseInt(timeout);
                settings.setTimeout(t);
            } else {
                settings.setTimeout(-1);
            }
        } catch (NumberFormatException nfe) {
            throw new InvalidSettingsException(
                    "Invalid timeout (please enter a number).");
        }

        settings.setUser(m_user.getText().trim());
        if (!m_passwordChanged) {
            String pass =
                    m_password.getPassword().length == 0 ? null : new String(
                            m_password.getPassword());
            settings.setEncryptPassword(pass);
        } else {
            try {
                char[] pass = m_password.getPassword();
                if (pass == null || pass.length == 0) {
                    settings.setEncryptPassword(null);
                } else {
                    settings.setEncryptPassword(KnimeEncryption.encrypt(pass));
                }
            } catch (Exception e) {
                String msg = "<no details>";
                if (e.getMessage() != null) {
                    msg = e.getMessage();
                }
                LOGGER.error("Encryption of password failed. Not stored! ("
                        + msg + ")", e);
                settings.setEncryptPassword(null);
            }
        }
        if (!m_keyPassphraseChanged) {
            String keyPhrase =
                    m_keyPassphrase.getPassword().length == 0 ? null
                            : new String(m_keyPassphrase.getPassword());
            settings.setEncryptKeyPassphrase(keyPhrase);
        } else {
            try {
                char[] pass = m_keyPassphrase.getPassword();
                if (pass == null || pass.length == 0) {
                    settings.setEncryptKeyPassphrase(null);
                } else {
                    settings.setEncryptKeyPassphrase(KnimeEncryption
                            .encrypt(pass));
                }
            } catch (Exception e) {
                String msg = "<no details>";
                if (e.getMessage() != null) {
                    msg = e.getMessage();
                }
                LOGGER.error("Encryption of passphrase failed. Not stored! ("
                        + msg + ")", e);
                settings.setEncryptKeyPassphrase(null);
            }
        }

        settings.setCommand(m_command.getText());
        settings.setRemoteInputFile(m_remoteInFile.getText());
        settings.setRemoteOutputFile(m_remoteOutFile.getText());
    }

    /**
     * Simply reads all values from the settings object and transfers them into
     * the dialog's components.
     *
     * @param settings the settings values to display
     */
    private void transferSettingsIntoComponents(
            final ExtSSHToolSettings settings) {

        m_host.setText(settings.getRemoteHost());

        if (settings.getPortNumber() >= 0) {
            m_port.setText("" + settings.getPortNumber());
        } else {
            m_port.setText("");
        }
        if (settings.getTimeout() > 0) {
            m_timeout.setText("" + settings.getTimeout());
        } else {
            m_timeout.setText("");
        }
        m_user.setText(settings.getUser());
        m_password.setText(settings.getEncryptPassword());
        m_passwordChanged = false;
        m_keyPassphrase.setText(settings.getEncryptKeyPassphrase());
        m_keyPassphraseChanged = false;
        m_command.setText(settings.getCommand());
        m_remoteInFile.setText(settings.getRemoteInputFile());
        m_remoteOutFile.setText(settings.getRemoteOutputFile());

    }

    private void checkConnection() {
        ExtSSHToolSettings s = new ExtSSHToolSettings();
        try {
            transferComponentsValuesIntoSettings(s);
            SSHUtil.getConnectedSession(s);
            JOptionPane.showMessageDialog(this, "Looks good.");
        } catch (InvalidSettingsException ise) {
            JOptionPane.showMessageDialog(this,
                    "Can't connect - invalid settings: " + ise.getMessage());
        } catch (Throwable t) {
            String msg = "<no details>";
            if (t.getMessage() != null && !t.getMessage().isEmpty()) {
                msg = t.getMessage();
            }
            JOptionPane.showMessageDialog(this, "Connection failed:\n" + msg);
        }
    }

}
