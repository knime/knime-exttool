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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 18, 2010 (wiswedel): created
 */
package org.knime.exttool.node;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.exttool.filetype.AbstractFileTypeFactory;
import org.knime.exttool.filetype.AbstractFileTypeReadConfig;
import org.knime.exttool.filetype.AbstractFileTypeReadConfigPanel;
import org.knime.exttool.node.ExttoolSettings.PathAndTypeConfigurationOutput;

/** Output file panel to the external tool node dialog. It shows temp file
 * paths and file type selection.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("serial")
public class OutputFilePanel extends JPanel {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(OutputFilePanel.class);

    private final ExttoolCustomizer m_exttoolCustomizer;
    private final PortSettingsPanel[] m_portSettingsPanels;

    private File m_lastDirectory;

    /** Creates new panel based on (read-only) customizer. Callers must call
     * the {@link #initLayout()} method immediately afterwards.
     * @param customizer The customizer, set up in the node factory.
     */
    public OutputFilePanel(final ExttoolCustomizer customizer) {
        super(new GridBagLayout());
        m_exttoolCustomizer = customizer;
        m_portSettingsPanels = new PortSettingsPanel[customizer.getNrOutputs()];
        for (int i = 0; i < m_portSettingsPanels.length; i++) {
            m_portSettingsPanels[i] = new PortSettingsPanel(i);
        }
    }

    /** Does the layout, to be called right after construction. */
    protected void initLayout() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        StringBuilder helpText = new StringBuilder("<html><body>");
        helpText.append("Output file of the executable. This file<br/>");
        helpText.append("is read by KNIME after successful execution.");
        JLabel label = new JLabel(helpText.toString());
        add(label, gbc);

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        for (int i = 0; i < m_portSettingsPanels.length; i++) {
            add(new JSeparator(), gbc);
            add(m_portSettingsPanels[i], gbc);
        }
    }

    /** Restores settings from a settings object, inits defaults if that fails.
     * @param settings To load from.
     * @throws NotConfigurableException If no valid configuration is possible
     *         (not actually thrown)
     */
    protected void loadSettingsFrom(final ExttoolSettings settings)
        throws NotConfigurableException {
        for (PortSettingsPanel p : m_portSettingsPanels) {
            p.loadSettingsFrom(settings);
        }
    }

    /** Saves the current configuration to the argument.
     * @param settings To save to.
     * @throws InvalidSettingsException If the current configuration is invalid.
     */
    protected void saveSettingsTo(final ExttoolSettings settings)
        throws InvalidSettingsException {
        for (int i = 0; i < m_portSettingsPanels.length; i++) {
            m_portSettingsPanels[i].saveSettingsTo(settings);
        }
    }

    private final class PortSettingsPanel extends JPanel {

        private final int m_portIndex;
        private final ButtonGroup m_typeButtonGroup;
        private final Map<String, AbstractFileTypeReadConfigPanel>
            m_type2ConfigPanelMap;
        private final JTextField m_outputFileField;
        private final JButton m_outputFileBrowseButton;
        private final JCheckBox m_autoGeneratedFileNameChecker;
        private final JPanel m_centerPanel;
        private Dimension m_centerPanelPrefSize;

        /** Creates new panel for given port.
         * @param portIndex The port
         */
        PortSettingsPanel(final int portIndex) {
            super(new BorderLayout());
            m_portIndex = portIndex;
            m_outputFileField = new JTextField(20);
            m_outputFileBrowseButton = new JButton("Browse...");
            m_outputFileBrowseButton.addActionListener(new ActionListener() {
            private final JFileChooser m_chooser = new JFileChooser();
                /** {@inheritDoc} */
                @Override
                public void actionPerformed(final ActionEvent e) {
                    String old = m_outputFileField.getText();
                    if (old != null && old.length() > 0) {
                        File parent = new File(old).getParentFile();
                        if (parent.isDirectory()) {
                            m_chooser.setCurrentDirectory(parent);
                        }
                    } else if (m_lastDirectory != null) {
                        m_chooser.setCurrentDirectory(m_lastDirectory);
                    }
                    int exit = m_chooser.showDialog(
                            OutputFilePanel.this, "Select");
                    if (exit == JFileChooser.APPROVE_OPTION) {
                        File file = m_chooser.getSelectedFile();
                        if (file != null) {
                            m_lastDirectory = file.getParentFile();
                            m_outputFileField.setText(file.getAbsolutePath());
                        }
                    }
                }
            });

            m_autoGeneratedFileNameChecker =
                new JCheckBox("Generate unique file name on execute");
            m_autoGeneratedFileNameChecker.addItemListener(new ItemListener() {
                /** {@inheritDoc} */
                @Override
                public void itemStateChanged(final ItemEvent e) {
                    boolean selected =
                        m_autoGeneratedFileNameChecker.isSelected();
                    m_outputFileField.setEnabled(!selected);
                    m_outputFileBrowseButton.setEnabled(!selected);
                }
            });
            m_autoGeneratedFileNameChecker.doClick();
            m_centerPanel = new JPanel(new BorderLayout());
            m_centerPanel.setBorder(BorderFactory.createTitledBorder(
                    " Type settings "));
            m_typeButtonGroup = new ButtonGroup();
            ActionListener typeChangeListener = new ActionListener() {
                /** {@inheritDoc} */
                @Override
                public void actionPerformed(final ActionEvent e) {
                    onTypeChange(e.getActionCommand());
                }
            };
            m_type2ConfigPanelMap =
                new HashMap<String, AbstractFileTypeReadConfigPanel>();
            AbstractButton first = null;
            for (AbstractFileTypeFactory readFac
                    : AbstractFileTypeFactory.getReadFactories()) {
                JRadioButton button =
                    new JRadioButton(readFac.getUserFriendlyName());
                if (first == null) {
                    first = button;
                }
                button.setActionCommand(readFac.getID());
                button.addActionListener(typeChangeListener);
                m_typeButtonGroup.add(button);
                AbstractFileTypeReadConfigPanel configPanel =
                    readFac.createNewReadConfig().createConfigPanel();
                m_type2ConfigPanelMap.put(readFac.getID(), configPanel);
            }
            initLayoutPortSettingsPanel();
            if (first != null) {
                first.doClick();
            }
        }

        /**
         *
         */
        private void initLayoutPortSettingsPanel() {
            JPanel typeButtonsPanel = new JPanel(
                    new FlowLayout(FlowLayout.LEFT));
            for (Enumeration<AbstractButton> enu
                    = m_typeButtonGroup.getElements(); enu.hasMoreElements();) {
                typeButtonsPanel.add(enu.nextElement());
            }
            add(typeButtonsPanel, BorderLayout.NORTH);
            add(m_centerPanel, BorderLayout.CENTER);
            if (m_exttoolCustomizer.isShowPathToTempOutputFile()) {
                JPanel southPanel = new JPanel(new GridBagLayout());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(5, 5, 5, 5);
                gbc.anchor = GridBagConstraints.WEST;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                southPanel.add(m_autoGeneratedFileNameChecker, gbc);

                gbc.gridwidth = 1;
                boolean hasMultipleIns = m_exttoolCustomizer.getNrOutputs() > 1;
                String name = "File Path" + (hasMultipleIns
                        ? " (Port " + m_portIndex + ")" : "");
                southPanel.add(new JLabel(name), gbc);
                gbc.fill = GridBagConstraints.HORIZONTAL;
                southPanel.add(m_outputFileField, gbc);

                gbc.fill = GridBagConstraints.NONE;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                southPanel.add(m_outputFileBrowseButton, gbc);
                add(southPanel, BorderLayout.SOUTH);
            }

        }

        /**
         * @param actionCommand
         */
        private void onTypeChange(final String actionCommand) {
            AbstractFileTypeReadConfigPanel panel =
                m_type2ConfigPanelMap.get(actionCommand);

            m_centerPanel.removeAll();
            if (m_centerPanelPrefSize != null) {
                panel.setPreferredSize(m_centerPanelPrefSize);
            }
            String type = "";
            try {
                type = AbstractFileTypeFactory.get(
                        actionCommand).getUserFriendlyName();
            } catch (InvalidSettingsException e) {
                LOGGER.warn("No type factory with action command "
                        + actionCommand);
            }
            m_centerPanel.setBorder(BorderFactory.createTitledBorder(
                    " " + type + " - Type Settings "));

            m_centerPanel.add(panel);
            m_centerPanel.revalidate();
            m_centerPanel.repaint();
        }

        /** @return the currently set read config panel. */
        private AbstractFileTypeReadConfigPanel getReadConfigPanel() {
            AbstractFileTypeReadConfigPanel confPanel =
                (AbstractFileTypeReadConfigPanel)m_centerPanel.getComponent(0);
            return confPanel;
        }

        /** Restores settings from a settings object, inits defaults if fails.
         * @param settings To load from.
         * @throws NotConfigurableException Not actually thrown but declared
         * as the load method in a dialog allows for it.
         */
        protected void loadSettingsFrom(final ExttoolSettings settings)
            throws NotConfigurableException {

            PathAndTypeConfigurationOutput config =
                settings.getOutputConfig(m_portIndex);
            String path = config.getPath();
            m_autoGeneratedFileNameChecker.setSelected(path == null);
            m_outputFileField.setText(path);

            AbstractFileTypeFactory desiredType = config.getType();
            AbstractFileTypeReadConfig desiredReadConfig =
                config.getReadConfig();
            if (desiredType == null) {
                desiredType =
                    PathAndTypeConfigurationOutput.getCSVTypeFactory();
                desiredReadConfig = desiredType.createNewReadConfig();
            } else {
                AbstractFileTypeReadConfig reference =
                    desiredType.createNewReadConfig();
                // config does not match type, use new default
                if (desiredReadConfig == null || !reference.getClass().equals(
                        desiredReadConfig.getClass())) {
                    // init defaults
                    reference.loadSettingsInDialog(
                            ExttoolSettings.EMPTY_SETTINGS);
                    desiredReadConfig = reference;
                }
            }
            AbstractButton desiredTypeButton = null;

            AbstractButton firstValidButton = null;
            AbstractFileTypeReadConfig firstValidConfig = null;

            int maxWidth = 0;
            int maxHeight = 0;
            for (AbstractButton button
                    : Collections.list(m_typeButtonGroup.getElements())) {
                String id = button.getActionCommand();
                AbstractFileTypeFactory temp;
                try {
                    temp = AbstractFileTypeFactory.get(id);
                } catch (InvalidSettingsException e1) {
                    LOGGER.error("Invalid read type: " + id, e1);
                    button.setEnabled(false);
                    continue;
                }
                // init default values for all types
                AbstractFileTypeReadConfig readConfig =
                    temp.createNewReadConfig();
                readConfig.loadSettingsInDialog(ExttoolSettings.EMPTY_SETTINGS);
                AbstractFileTypeReadConfigPanel panel =
                    m_type2ConfigPanelMap.get(id);
                panel.loadSettings(readConfig);
                Dimension dim = panel.getPreferredSize();
                maxHeight = Math.max(maxHeight, dim.height);
                maxWidth = Math.max(maxHeight, dim.width);

                if (firstValidButton == null) {
                    firstValidButton = button;
                    firstValidConfig = readConfig;
                }
                if (temp.getID().equals(desiredType.getID())) {
                    desiredTypeButton = button;
                }
            }
            m_centerPanelPrefSize = new Dimension(maxWidth, maxHeight);
            if (firstValidButton == null) {
                String message = "No valid output type available";
                LOGGER.error(message);
                throw new NotConfigurableException(message);
            }
            AbstractButton b;
            AbstractFileTypeReadConfig c;
            if (desiredTypeButton != null) {
                b = desiredTypeButton;
                c = desiredReadConfig;
            } else {
                b = firstValidButton;
                c = firstValidConfig;
            }
            b.doClick();
            getReadConfigPanel().loadSettings(c);
        }


        /** Saves the current configuration to the argument.
         * @param settings To save to.
         * @throws InvalidSettingsException
         *  If the current configuration is invalid.
         */
        protected void saveSettingsTo(final ExttoolSettings settings)
            throws InvalidSettingsException {
            PathAndTypeConfigurationOutput config =
                new PathAndTypeConfigurationOutput();
            if (m_autoGeneratedFileNameChecker.isSelected()) {
                config.setPath(null);
            } else {
                String p = m_outputFileField.getText();
                if (p == null || p.length() == 0) {
                    StringBuilder b = new StringBuilder("No output file path");
                    if (m_exttoolCustomizer.getNrOutputs() > 1) {
                        b.append(" for port ").append(m_portIndex);
                    }
                    b.append(" specified");
                    throw new InvalidSettingsException(b.toString());
                }
                config.setPath(p);
            }
            String id = m_typeButtonGroup.getSelection().getActionCommand();
            AbstractFileTypeFactory fac = AbstractFileTypeFactory.get(id);
            AbstractFileTypeReadConfig readConf = fac.createNewReadConfig();
            AbstractFileTypeReadConfigPanel confPanel = getReadConfigPanel();
            confPanel.saveSettings(readConf);
            config.setType(fac);
            config.setReadConfig(readConf);
            settings.setOutputConfig(m_portIndex, config);
        }
    }

}
