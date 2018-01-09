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
 * Created on Oct 11, 2013 by Patrick Winter, KNIME AG, Zurich, Switzerland
 */
package org.knime.ext.ssh.node;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.KnimeEncryption;
import org.knime.ext.ssh.SSHUtil;

/**
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
public class ExtSSHToolSettingsPanel extends JPanel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ExtSSHToolSettingsPanel.class);

    private static final long serialVersionUID = -752825081032880103L;

    private JTextField m_host = new JTextField();

    private JSpinner m_port = new JSpinner(new SpinnerNumberModel(ExtSSHToolSettings.DEFAULT_SSH_PORT, 0, 65535, 1));

    private JTextField m_timeout = new JTextField();

    private JTextField m_user = new JTextField();

    private JPasswordField m_password = new JPasswordField();

    private JPasswordField m_passphrase = new JPasswordField();

    private JTextField m_command = new JTextField();

    private JTextField m_remoteInFile = new JTextField();

    private JTextField m_remoteOutFile = new JTextField();

    private JCheckBox m_useKnownHosts = new JCheckBox();

    private boolean m_passwordChanged = false;

    private boolean m_passphraseChanged = false;

    /**
     * Creates the configuration panel.
     */
    ExtSSHToolSettingsPanel() {
        m_password.addFocusListener(new FocusAdapter() {
            /** {@inheritDoc} */
            @Override
            public void focusGained(final FocusEvent e) {
                m_password.setText("");
                m_passwordChanged = true;
            }
        });
        m_password.setToolTipText("Leave empty if not needed");
        m_passphrase.addFocusListener(new FocusAdapter() {
            /** {@inheritDoc} */
            @Override
            public void focusGained(final FocusEvent e) {
                m_passphrase.setText("");
                m_passphraseChanged = true;
            }
        });
        m_passphrase.setToolTipText("Leave empty if not used or required");
        m_useKnownHosts.setText("Use known host (as configured in the \"SSH2\" preference page)");
        m_useKnownHosts.setSelected(true);
        initPanel();
    }

    private void initPanel() {
        JLabel hostLabel = new JLabel("Host:");
        JLabel portLabel = new JLabel("Port:");
        JLabel timeoutLabel = new JLabel("Timeout (seconds):");
        JLabel userLabel = new JLabel("Username:");
        JLabel passwordLabel = new JLabel("Password:");
        JLabel passphraseLabel = new JLabel("Passphrase (key file):");
        JLabel commandLabel = new JLabel("Remote command:");
        JLabel remoteInFileLabel = new JLabel("Remote input file:");
        JLabel remoteOutFileLabel = new JLabel("Remote output file:");
        JButton checkConnection = new JButton("Check Connection");
        checkConnection.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                checkConnection();
            }
        });
        GridBagConstraints gbc = new GridBagConstraints();
        // Connection panel
        JPanel connectionPanel = new JPanel(new GridBagLayout());
        connectionPanel.setBorder(new TitledBorder(new EtchedBorder(), "Connection Information:"));
        resetGBC(gbc);
        connectionPanel.add(hostLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        connectionPanel.add(m_host, gbc);
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridy++;
        connectionPanel.add(portLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        connectionPanel.add(m_port, gbc);
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridy++;
        connectionPanel.add(timeoutLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        connectionPanel.add(m_timeout, gbc);
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridy++;
        connectionPanel.add(userLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        connectionPanel.add(m_user, gbc);
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridy++;
        connectionPanel.add(passwordLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        connectionPanel.add(m_password, gbc);
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridy++;
        connectionPanel.add(passphraseLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        connectionPanel.add(m_passphrase, gbc);
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        connectionPanel.add(m_useKnownHosts, gbc);
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        connectionPanel.add(checkConnection, gbc);
        // Remote panel
        JPanel remotePanel = new JPanel(new GridBagLayout());
        remotePanel.setBorder(new TitledBorder(new EtchedBorder(), "Remote Command:"));
        resetGBC(gbc);
        remotePanel.add(commandLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        remotePanel.add(m_command, gbc);
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridy++;
        remotePanel.add(remoteInFileLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        remotePanel.add(m_remoteInFile, gbc);
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridy++;
        remotePanel.add(remoteOutFileLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        remotePanel.add(m_remoteOutFile, gbc);
        // Outer panel
        this.setLayout(new GridBagLayout());
        resetGBC(gbc);
        gbc.weightx = 1;
        this.add(connectionPanel, gbc);
        gbc.gridy++;
        this.add(remotePanel, gbc);
    }

    /**
     * Load the settings.
     *
     * @param settings Object containing the settings.
     */
    void loadSettings(final ExtSSHToolSettings settings) {
        m_host.setText(settings.getRemoteHost());
        if (settings.getPortNumber() >= 0) {
            m_port.setValue(settings.getPortNumber());
        } else {
            m_port.setValue(ExtSSHToolSettings.DEFAULT_SSH_PORT);
        }
        if (settings.getTimeout() > 0) {
            m_timeout.setText("" + settings.getTimeout());
        } else {
            m_timeout.setText("");
        }
        m_user.setText(settings.getUser());
        m_password.setText(settings.getEncryptPassword());
        m_passwordChanged = false;
        m_passphrase.setText(settings.getEncryptKeyPassphrase());
        m_passphraseChanged = false;
        m_command.setText(settings.getCommand());
        m_remoteInFile.setText(settings.getRemoteInputFile());
        m_remoteOutFile.setText(settings.getRemoteOutputFile());
        m_useKnownHosts.setSelected(!settings.getDisableKnownHosts());
    }

    /**
     * Save the current settings.
     *
     * @param settings Object to put the settings in.
     * @throws InvalidSettingsException If the current configuration is invalid.
     */
    void saveSettings(final ExtSSHToolSettings settings) throws InvalidSettingsException {
        settings.setRemoteHost(m_host.getText().trim());
        settings.setPortNumber((Integer)m_port.getValue());
        try {
            String timeout = m_timeout.getText().trim();
            if (!timeout.isEmpty()) {
                int t = Integer.parseInt(timeout);
                settings.setTimeout(t);
            } else {
                settings.setTimeout(-1);
            }
        } catch (NumberFormatException nfe) {
            throw new InvalidSettingsException("Invalid timeout (please enter a number).");
        }
        settings.setUser(m_user.getText().trim());
        if (!m_passwordChanged) {
            String pass = m_password.getPassword().length == 0 ? null : new String(m_password.getPassword());
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
                LOGGER.error("Encryption of password failed. Not stored! (" + msg + ")", e);
                settings.setEncryptPassword(null);
            }
        }
        if (!m_passphraseChanged) {
            String keyPhrase = m_passphrase.getPassword().length == 0 ? null : new String(m_passphrase.getPassword());
            settings.setEncryptKeyPassphrase(keyPhrase);
        } else {
            try {
                char[] pass = m_passphrase.getPassword();
                if (pass == null || pass.length == 0) {
                    settings.setEncryptKeyPassphrase(null);
                } else {
                    settings.setEncryptKeyPassphrase(KnimeEncryption.encrypt(pass));
                }
            } catch (Exception e) {
                String msg = "<no details>";
                if (e.getMessage() != null) {
                    msg = e.getMessage();
                }
                LOGGER.error("Encryption of passphrase failed. Not stored! (" + msg + ")", e);
                settings.setEncryptKeyPassphrase(null);
            }
        }
        settings.setCommand(m_command.getText());
        settings.setRemoteInputFile(m_remoteInFile.getText());
        settings.setRemoteOutputFile(m_remoteOutFile.getText());
        settings.setDisableKnownHosts(!m_useKnownHosts.isSelected());
    }

    private void checkConnection() {
        ExtSSHToolSettings settings = new ExtSSHToolSettings();
        try {
            saveSettings(settings);
            SSHUtil.getConnectedSession(settings);
            JOptionPane.showMessageDialog(this, "Looks good.");
        } catch (InvalidSettingsException ise) {
            JOptionPane.showMessageDialog(this, "Can't connect - invalid settings: " + ise.getMessage());
        } catch (Throwable t) {
            String msg = "<no details>";
            if (t.getMessage() != null && !t.getMessage().isEmpty()) {
                msg = t.getMessage();
            }
            JOptionPane.showMessageDialog(this, "Connection failed:\n" + msg);
        }
    }

    private static void resetGBC(final GridBagConstraints gbc) {
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
    }

}
