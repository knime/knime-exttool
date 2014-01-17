/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilter;
import org.knime.exttool.filetype.AbstractFileTypeFactory;
import org.knime.exttool.filetype.AbstractFileTypeWriteConfig;
import org.knime.exttool.filetype.AbstractFileTypeWriteConfigPanel;
import org.knime.exttool.node.ExttoolSettings.PathAndTypeConfigurationInput;

/** Input file panel to the external tool node dialog. It shows temp file
 * paths and file type selection.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
@SuppressWarnings("serial")
public class InputFilePanel extends JPanel {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(InputFilePanel.class);

    private final List<ChangeListener> m_listenerList =
        new ArrayList<ChangeListener>();

    private final ExttoolCustomizer m_exttoolCustomizer;
    private final PortSettingsPanel[] m_portSettingsPanels;

    private File m_lastDirectory;

    /** Creates new panel based on (read-only) customizer. Callers must call
     * the {@link #initLayout()} method immediately afterwards.
     * @param customizer The customizer, set up in the node factory.
     */
    public InputFilePanel(final ExttoolCustomizer customizer) {
        super(new GridBagLayout());
        m_exttoolCustomizer = customizer;
        m_portSettingsPanels = new PortSettingsPanel[customizer.getNrInputs()];
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
        helpText.append("Input file to the executable. The file is written ");
        helpText.append("by KNIME <br />");
        helpText.append("prior execution and passed to the executable.");
        JLabel label = new JLabel(helpText.toString());
        add(label, gbc);

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        for (int i = 0; i < m_portSettingsPanels.length; i++) {
            add(new JSeparator(), gbc);
            add(m_portSettingsPanels[i], gbc);
        }
    }

    /** Called when a new column is selected.
     * @param newSelected The newly selected column.
     */
    protected void onColumnSelectionChanged(final DataColumnSpec newSelected) {
        for (ChangeListener l : m_listenerList) {
            l.stateChanged(new ChangeEvent(this));
        }
    }

    /** Add a change listener to this panel, to be informed when input type
     * changes.
     * @param listener The listener to be registered.
     */
    public void addChangeListener(final ChangeListener listener) {
        if (listener == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        if (!m_listenerList.contains(listener)) {
            m_listenerList.add(listener);
        }
    }

    /** Remove a change listener from the listener list.
     * @param listener To be removed.
     */
    public void removeChangeListener(final ChangeListener listener) {
        m_listenerList.remove(listener);
    }

    /** Restores settings from a settings object, inits defaults if that fails.
     * @param settings To load from.
     * @param inputSpecs The input table specs.
     * @throws NotConfigurableException If no valid configuration is possible.
     */
    protected void loadSettingsFrom(final ExttoolSettings settings,
            final DataTableSpec[] inputSpecs) throws NotConfigurableException {
        for (int i = 0; i < m_portSettingsPanels.length; i++) {
            m_portSettingsPanels[i].loadSettingsFrom(settings, inputSpecs[i]);
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

    /** A column filter to filter out all columns in the column selection
     * combo box that are not supported by the selected file type.
     */
    static class SupportedTypesColumnFilter implements ColumnFilter {

        private final ColumnFilter m_baseFilter;

        /** Create new instance, refining the argument filter (passed
         * from the {@link ExttoolCustomizer#getColumnFilter()}.
         * @param filter The filter to refine.
         */
        public SupportedTypesColumnFilter(final ColumnFilter filter) {
            if (filter == null) {
                throw new NullPointerException("Argument must not be null.");
            }
            m_baseFilter = filter;
        }

        /** {@inheritDoc} */
        @Override
        public String allFilteredMsg() {
            return "No appropriate file type associated "
                + "with any of the input columns";
        }

        /** {@inheritDoc} */
        @Override
        public boolean includeColumn(final DataColumnSpec colSpec) {
            if (!m_baseFilter.includeColumn(colSpec)) {
                return false;
            }
            for (AbstractFileTypeFactory fac
                    : AbstractFileTypeFactory.getWriteFactories()) {
                if (fac.accepts(colSpec)) {
                    return true;
                }
            }
            return false;
        }
    }

    private final class PortSettingsPanel extends JPanel {

        private final int m_portIndex;
        private final ButtonGroup m_typeButtonGroup;
        private final Map<String, AbstractFileTypeWriteConfigPanel>
            m_type2ConfigPanelMap;
        private final JTextField m_inputFileField;
        private final JButton m_inputFileBrowseButton;
        private final JCheckBox m_autoGeneratedFileNameChecker;
        private final JPanel m_centerPanel;
        private Dimension m_centerPanelPrefSize;

        /** Creates new panel for given port.
         * @param portIndex The port.
         */
        PortSettingsPanel(final int portIndex) {
            super(new BorderLayout());
            m_portIndex = portIndex;
            m_inputFileField = new JTextField(20);
            m_inputFileBrowseButton = new JButton("Browse...");
            m_inputFileBrowseButton.addActionListener(new ActionListener() {
            private final JFileChooser m_chooser = new JFileChooser();
                /** {@inheritDoc} */
                @Override
                public void actionPerformed(final ActionEvent e) {
                    String old = m_inputFileField.getText();
                    if (old != null && old.length() > 0) {
                        File parent = new File(old).getParentFile();
                        if (parent.isDirectory()) {
                            m_chooser.setCurrentDirectory(parent);
                        }
                    } else if (m_lastDirectory != null) {
                        m_chooser.setCurrentDirectory(m_lastDirectory);
                    }
                    int exit = m_chooser.showDialog(
                            InputFilePanel.this, "Select");
                    if (exit == JFileChooser.APPROVE_OPTION) {
                        File selFile = m_chooser.getSelectedFile();
                        if (selFile != null) {
                            m_lastDirectory = selFile.getParentFile();
                            m_inputFileField.setText(selFile.getAbsolutePath());
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
                    m_inputFileField.setEnabled(!selected);
                    m_inputFileBrowseButton.setEnabled(!selected);
                }
            });
            m_autoGeneratedFileNameChecker.doClick();
            m_centerPanel = new JPanel(new BorderLayout());
            m_typeButtonGroup = new ButtonGroup();
            ActionListener typeChangeListener = new ActionListener() {
                /** {@inheritDoc} */
                @Override
                public void actionPerformed(final ActionEvent e) {
                    onTypeChange(e.getActionCommand());
                }
            };
            m_type2ConfigPanelMap =
                new HashMap<String, AbstractFileTypeWriteConfigPanel>();
            AbstractButton first = null;
            for (AbstractFileTypeFactory writeFac
                    : AbstractFileTypeFactory.getWriteFactories()) {
                JRadioButton button =
                    new JRadioButton(writeFac.getUserFriendlyName());
                if (first == null) {
                    first = button;
                }
                button.setActionCommand(writeFac.getID());
                button.addActionListener(typeChangeListener);
                m_typeButtonGroup.add(button);
                AbstractFileTypeWriteConfigPanel configPanel =
                    writeFac.createNewWriteConfig().createConfigPanel();
                m_type2ConfigPanelMap.put(writeFac.getID(), configPanel);
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
            if (m_exttoolCustomizer.isShowOutputTypeSelection()) {
                for (Enumeration<AbstractButton> enu =
                    m_typeButtonGroup.getElements(); enu.hasMoreElements();) {
                    typeButtonsPanel.add(enu.nextElement());
                }
            }
            add(typeButtonsPanel, BorderLayout.NORTH);
            add(m_centerPanel, BorderLayout.CENTER);
            if (m_exttoolCustomizer.isShowPathToTempInputFile()) {
                JPanel southPanel = new JPanel(new GridBagLayout());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(5, 5, 5, 5);
                gbc.anchor = GridBagConstraints.WEST;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                southPanel.add(m_autoGeneratedFileNameChecker, gbc);

                gbc.gridwidth = 1;
                boolean hasMultipleIns = m_exttoolCustomizer.getNrInputs() > 1;
                String name = "File Path" + (hasMultipleIns
                        ? " (Port " + m_portIndex + ")" : "");
                southPanel.add(new JLabel(name), gbc);
                gbc.fill = GridBagConstraints.HORIZONTAL;
                southPanel.add(m_inputFileField, gbc);

                gbc.fill = GridBagConstraints.NONE;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                southPanel.add(m_inputFileBrowseButton, gbc);
                add(southPanel, BorderLayout.SOUTH);
            }

        }

        /**
         * @param actionCommand
         */
        private void onTypeChange(final String actionCommand) {
            AbstractFileTypeWriteConfigPanel panel =
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

        /** @return the currently set write config panel. */
        private AbstractFileTypeWriteConfigPanel getWriteConfigPanel() {
            AbstractFileTypeWriteConfigPanel confPanel =
                (AbstractFileTypeWriteConfigPanel)m_centerPanel.getComponent(0);
            return confPanel;
        }

        /** Restores settings from a settings object, inits defaults if fails.
         * @param settings To load from.
         * @param inputSpec The input table spec associated with this port.
         * @throws NotConfigurableException If no valid configuration possible
         */
        protected void loadSettingsFrom(final ExttoolSettings settings,
                final DataTableSpec inputSpec)
            throws NotConfigurableException {

            PathAndTypeConfigurationInput config =
                settings.getInputConfig(m_portIndex);
            String path = config.getPath();
            m_autoGeneratedFileNameChecker.setSelected(path == null);
            m_inputFileField.setText(path);

            AbstractFileTypeFactory desiredType = config.getType();
            AbstractFileTypeWriteConfig desiredWriteConfig =
                config.getWriteConfig();
            if (desiredType == null) {
                desiredType = PathAndTypeConfigurationInput.getCSVTypeFactory();
                desiredWriteConfig = desiredType.createNewWriteConfig();
            } else {
                AbstractFileTypeWriteConfig reference =
                    desiredType.createNewWriteConfig();
                // config does not match type, use new default
                if (desiredWriteConfig == null || !reference.getClass().equals(
                        desiredWriteConfig.getClass())) {
                    // init defaults on reference
                    try {
                        reference.loadSettingsInDialog(
                                ExttoolSettings.EMPTY_SETTINGS, inputSpec);
                    } catch (NotConfigurableException nce) {
                        // ignore here.
                    }
                    desiredWriteConfig = reference;
                }
            }
            AbstractButton firstValidButton = null;
            AbstractFileTypeWriteConfig firstValidConfig = null;
            AbstractButton desiredTypeButton = null;

            // disable all types which cannot be used (not configurable)
            int maxWidth = 0;
            int maxHeight = 0;
            for (AbstractButton button
                    : Collections.list(m_typeButtonGroup.getElements())) {
                String id = button.getActionCommand();
                AbstractFileTypeFactory temp;
                try {
                    temp = AbstractFileTypeFactory.get(id);
                } catch (InvalidSettingsException e1) {
                    LOGGER.error("Invalid write type: " + id, e1);
                    button.setEnabled(false);
                    continue;
                }
                AbstractFileTypeWriteConfig c = temp.createNewWriteConfig();
                AbstractFileTypeWriteConfigPanel panel =
                    m_type2ConfigPanelMap.get(id);
                boolean enabled = true;
                try {
                    c.loadSettingsInDialog(
                            ExttoolSettings.EMPTY_SETTINGS, inputSpec);
                    panel.loadSettings(c, inputSpec);
                } catch (NotConfigurableException e) {
                    enabled = false;
                    LOGGER.debug("Disabling input type \""
                            + temp.getUserFriendlyName() + "\" for port "
                            + m_portIndex + ": " + e.getMessage());
                }
                Dimension dim = panel.getPreferredSize();
                maxHeight = Math.max(maxHeight, dim.height);
                maxWidth = Math.max(maxHeight, dim.width);

                button.setEnabled(enabled);
                if (enabled && firstValidButton == null) {
                    firstValidButton = button;
                    firstValidConfig = c;
                }
                if (enabled && id.equals(desiredType.getID())) {
                    desiredTypeButton = button;
                }
            }
            m_centerPanelPrefSize = new Dimension(maxWidth, maxHeight);
            if (firstValidButton == null) {
                throw new NotConfigurableException(
                        "No valid file type for input");
            }
            AbstractButton b;
            AbstractFileTypeWriteConfig c;
            if (desiredTypeButton != null) {
                b = desiredTypeButton;
                c = desiredWriteConfig;
            } else {
                b = firstValidButton;
                c = firstValidConfig;
            }
            b.doClick();
            getWriteConfigPanel().loadSettings(c, inputSpec);
        }

        /** Saves the current configuration to the argument.
         * @param settings To save to.
         * @throws InvalidSettingsException
         *  If the current configuration is invalid.
         */
        protected void saveSettingsTo(final ExttoolSettings settings)
            throws InvalidSettingsException {
            PathAndTypeConfigurationInput config =
                new PathAndTypeConfigurationInput();
            if (m_autoGeneratedFileNameChecker.isSelected()) {
                config.setPath(null);
            } else {
                String p = m_inputFileField.getText();
                if (p == null || p.length() == 0) {
                    StringBuilder b = new StringBuilder("No input file path");
                    if (m_exttoolCustomizer.getNrInputs() > 1) {
                        b.append(" for port ").append(m_portIndex);
                    }
                    b.append(" specified");
                    throw new InvalidSettingsException(b.toString());
                }
                config.setPath(p);
            }
            String id = m_typeButtonGroup.getSelection().getActionCommand();
            AbstractFileTypeFactory fac = AbstractFileTypeFactory.get(id);
            AbstractFileTypeWriteConfig writeConf = fac.createNewWriteConfig();
            AbstractFileTypeWriteConfigPanel confPanel = getWriteConfigPanel();
            confPanel.saveSettings(writeConf);
            config.setType(fac);
            config.setWriteConfig(writeConf);
            settings.setInputConfig(m_portIndex, config);
        }
    }

}
