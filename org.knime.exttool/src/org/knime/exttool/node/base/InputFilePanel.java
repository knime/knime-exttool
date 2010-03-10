/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
package org.knime.exttool.node.base;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.exttool.filetype.AbstractFileTypeFactory;
import org.knime.exttool.filetype.AbstractFileTypeWrite;
import org.knime.exttool.node.base.ExttoolSettings.PathAndTypeConfiguration;

/**
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class InputFilePanel extends JPanel {

    private final List<ChangeListener> m_listenerList =
        new ArrayList<ChangeListener>();

    private final ExttoolCustomizer m_exttoolCustomizer;

    private final ColumnSelectionComboxBox m_columnSelectionBox;
    private final SupportedTypesColumnFilter m_columnFilter;
    private final JCheckBox m_autoGeneratedFileNameChecker;
    private final JTextField[] m_inputFileFields;
    private final JButton[] m_inputFileBrowseButtons;
    private File m_lastDirectory;

    /**
     *
     */
    public InputFilePanel(final ExttoolCustomizer customizer) {
        super(new GridBagLayout());
        m_exttoolCustomizer = customizer;
        m_columnFilter =
            new SupportedTypesColumnFilter(customizer.getColumnFilter());
        m_columnSelectionBox =
            new ColumnSelectionComboxBox((Border)null, m_columnFilter);
        m_columnSelectionBox.addItemListener(new ItemListener() {
            /** {@inheritDoc} */
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    onColumnSelectionChanged((DataColumnSpec)e.getItem());
                }
            }
        });

        m_autoGeneratedFileNameChecker =
            new JCheckBox("Generate unique file name on execute");
        m_inputFileFields = new JTextField[customizer.getNrInputs()];
        m_inputFileBrowseButtons = new JButton[customizer.getNrInputs()];
        for (int i = 0; i < m_inputFileFields.length; i++) {
            final JTextField textField = new JTextField(20);
            m_inputFileFields[i] = textField;
            m_inputFileBrowseButtons[i] = new JButton("Browse...");
            m_inputFileBrowseButtons[i].addActionListener(new ActionListener() {
                private final JFileChooser m_chooser = new JFileChooser();

                /** {@inheritDoc} */
                @Override
                public void actionPerformed(final ActionEvent e) {
                    String old = textField.getText();
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
                            textField.setText(selFile.getAbsolutePath());
                        }
                    }
                }
            });
        }

        m_autoGeneratedFileNameChecker.addItemListener(new ItemListener() {
            /** {@inheritDoc} */
            public void itemStateChanged(final ItemEvent e) {
                boolean selected = m_autoGeneratedFileNameChecker.isSelected();
                for (int i = 0; i < m_inputFileFields.length; i++) {
                    m_inputFileFields[i].setEnabled(!selected);
                    m_inputFileBrowseButtons[i].setEnabled(!selected);
                }
            }
        });
        m_autoGeneratedFileNameChecker.doClick();
    }

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
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(new JSeparator(), gbc);
        gbc.fill = GridBagConstraints.NONE;

        if (m_exttoolCustomizer.isShowTargetColumnCombo()) {
            gbc.gridwidth = 1;
            gbc.gridx = 0;
            JLabel targetColLabel = new JLabel("Target Column");
            add(targetColLabel, gbc);

            gbc.gridx = GridBagConstraints.RELATIVE;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            add(m_columnSelectionBox, gbc);
        }

        if (m_exttoolCustomizer.isShowPathToTempInputFile()) {
            add(m_autoGeneratedFileNameChecker, gbc);
            for (int i = 0; i < m_inputFileFields.length; i++) {
                gbc.gridwidth = 1;
                String name = "File Path" + ((m_inputFileFields.length > 1)
                    ? " (Port " + i + ")" : "");
                add(new JLabel(name), gbc);
                add(m_inputFileFields[i], gbc);

                gbc.gridwidth = GridBagConstraints.REMAINDER;
                add(m_inputFileBrowseButtons[i], gbc);
            }
        }

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(new JSeparator(), gbc);
        gbc.fill = GridBagConstraints.NONE;
    }

    protected void onColumnSelectionChanged(final DataColumnSpec newSelected) {
        for (ChangeListener l : m_listenerList) {
            l.stateChanged(new ChangeEvent(this));
        }
    }

    public void addChangeListener(final ChangeListener listener) {
        if (listener == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        if (!m_listenerList.contains(listener)) {
            m_listenerList.add(listener);
        }
    }

    public void removeChangeListener(final ChangeListener listener) {
        m_listenerList.remove(listener);
    }

    protected void loadSettingsFrom(final ExttoolSettings settings,
            final DataTableSpec[] inputSpecs) throws NotConfigurableException {
        String selColumn = settings.getTargetColumn();
        DataTableSpec spec = inputSpecs.length > 0 ? inputSpecs[0] : null;
        m_columnSelectionBox.update(spec, selColumn);
        for (int i = 0; i < m_inputFileFields.length; i++) {
            PathAndTypeConfiguration config = settings.getInputConfig(i);
            String path = config.getPath();
            m_autoGeneratedFileNameChecker.setSelected(path == null);
            m_inputFileFields[i].setText(path);
        }
    }

    protected void saveSettingsTo(final ExttoolSettings settings)
        throws InvalidSettingsException {
        DataColumnSpec colSpec =
            (DataColumnSpec)m_columnSelectionBox.getSelectedItem();
        String selColumn = colSpec.getName();
        settings.setTargetColumn(selColumn);
        boolean isAuto = m_autoGeneratedFileNameChecker.isSelected();
        for (int i = 0; i < m_inputFileFields.length; i++) {
            PathAndTypeConfiguration config =
                new PathAndTypeConfiguration(true);
            if (isAuto) {
                config.setPath(null);
            } else {
                String p = m_inputFileFields[i].getText();
                if (p == null || p.length() == 0) {
                    StringBuilder b = new StringBuilder("No input file path");
                    if (m_inputFileFields.length > 1) {
                        b.append(" for port ").append(i);
                    }
                    b.append(" specified");
                    throw new InvalidSettingsException(b.toString());
                }
                config.setPath(p);
            }
            for (AbstractFileTypeFactory fac
                    : AbstractFileTypeFactory.getWriteFactories()) {
                if (fac.accepts(colSpec)) {
                    AbstractFileTypeWrite fileType =
                        fac.createNewWriteInstance();
                    fileType.setSelectedInput(colSpec);
                    NodeSettings saveSettings = new NodeSettings("port" + i);
                    fileType.saveSettings(saveSettings);
                    config.setType(fac.getID());
                    config.setTypeSettings(saveSettings);
                    break;
                }
            }
            settings.setInputConfig(i, config);
        }

    }

    public String getInputFileSummary(final int port) {
        DataColumnSpec colSpec =
            (DataColumnSpec)m_columnSelectionBox.getSelectedItem();
        if (colSpec == null) {
            return "no input selected, choose in corresponding tab";
        }
        for (AbstractFileTypeFactory factory
                : AbstractFileTypeFactory.getWriteFactories()) {
            if (factory.accepts(colSpec)) {
                return factory.getUserFriendlyName() + " on column \""
                    + colSpec.getName() + "\"";
            }
        }
        return "Nothing selected in input tab";
    }

    private static class SupportedTypesColumnFilter implements ColumnFilter {

        private final ColumnFilter m_baseFilter;

        /**
         *
         */
        public SupportedTypesColumnFilter(final ColumnFilter filter) {
            if (filter == null) {
                throw new NullPointerException("Argument must not be null.");
            }
            m_baseFilter = filter;
        }

        /** {@inheritDoc} */
        public String allFilteredMsg() {
            return "No appropriate file type associated "
                + "with any of the input columns";
        }

        /** {@inheritDoc} */
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

    public static void main(final String[] args) {
        JFrame frame = new JFrame("Test " + InputFilePanel.class);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        InputFilePanel panel = new InputFilePanel(new ExttoolCustomizer());
        panel.initLayout();
        frame.getContentPane().add(panel);
        frame.pack();
        frame.setVisible(true);
    }

}
