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
package org.knime.exttool.node;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.exttool.node.ExttoolCustomizer.Chunking;

/**
 * Main panel of the external tool node. It has fields for the executable,
 * the chunking properties and {@link AbstractCommandlineControl
 * command line controls}. Some of this panels can be switched on/off depending
 * upon their setting in the {@link ExttoolCustomizer}.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
@SuppressWarnings("serial")
public class ExternalToolPanel extends JPanel {

    private final FilesHistoryPanel m_executablePanel;
    private final JRadioButton m_chunkButtonEntireTable;
    private final JRadioButton m_chunkButtonSingleRow;
    private final JRadioButton m_chunkButtonChunkSize;
    private final JRadioButton m_chunkButtonNrChunks;
    private final JSpinner m_chunkSizeSpinner;
    private final JSpinner m_nrChunksSpinner;
    private final JLabel[] m_inputTypeSummaryLabels;

    private final ExttoolCustomizer m_exttoolCustomizer;
    private final AbstractCommandlineControl m_commandlineControl;


    /** Create new panel given a customizer. The caller needs to invoke
     * {@link #initLayout()} afterwards.
     * @param customizer The (read-only) customization object. */
    public ExternalToolPanel(final ExttoolCustomizer customizer) {
        super(new GridBagLayout());
        m_exttoolCustomizer = customizer;
        AbstractCommandlineSettings commandlineSettings =
            customizer.createCommandlineSettings();
        m_commandlineControl = commandlineSettings.createControl();
        String historyID = customizer.getExecutableFileHistoryID();
        String[] suffixes = customizer.getExecutableFileHistorySuffixes();
        m_executablePanel = new FilesHistoryPanel(historyID, suffixes);

        m_chunkButtonEntireTable = new JRadioButton("Entire Table");
        m_chunkButtonChunkSize = new JRadioButton("Chunks of Size");
        m_chunkButtonNrChunks = new JRadioButton("Nr of Chunks");
        m_chunkButtonSingleRow = new JRadioButton("Each row individually");
        m_chunkSizeSpinner = new JSpinner(
                new SpinnerNumberModel(500, 2, Integer.MAX_VALUE, 50));
        m_chunkButtonChunkSize.addChangeListener(new ChangeListener() {
            /** {@inheritDoc} */
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_chunkSizeSpinner.setEnabled(
                        m_chunkButtonChunkSize.isSelected());
            }
        });
        m_chunkSizeSpinner.setEnabled(false);
        m_nrChunksSpinner = new JSpinner(
                new SpinnerNumberModel(20, 1, Integer.MAX_VALUE, 2));
        m_chunkButtonNrChunks.addChangeListener(new ChangeListener() {
            /** {@inheritDoc} */
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_nrChunksSpinner.setEnabled(
                        m_chunkButtonNrChunks.isSelected());
            }
        });
        m_nrChunksSpinner.setEnabled(false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(m_chunkButtonEntireTable);
        bg.add(m_chunkButtonChunkSize);
        bg.add(m_chunkButtonNrChunks);
        bg.add(m_chunkButtonSingleRow);
        m_chunkButtonEntireTable.doClick();
        m_inputTypeSummaryLabels = new JLabel[customizer.getNrInputs()];
        for (int i = 0; i < m_inputTypeSummaryLabels.length; i++) {
            m_inputTypeSummaryLabels[i] = new JLabel(" ");
        }
    }

    /** Does the panel layout, must be called right after construction. */
    protected void initLayout() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        if (m_exttoolCustomizer.isShowPathToExecutableField()) {
            gbc.gridx = 0;
            JLabel label = new JLabel("Path to Executable");
            add(label, gbc);

            gbc.gridx = GridBagConstraints.RELATIVE;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            add(m_executablePanel, gbc);
        }

        if (m_exttoolCustomizer.isShowTabInputFile()) {
            for (int i = 0; i < m_inputTypeSummaryLabels.length; i++) {
                gbc.gridx = 0;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                add(m_inputTypeSummaryLabels[i], gbc);
            }
        }

        gbc.gridx = 0;
        gbc.gridwidth = 1;
        m_commandlineControl.registerPanel(this, gbc);

        if (m_exttoolCustomizer.isShowChunkSizeHandlingPanel()) {
            gbc.gridx = 0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            JPanel chunkHandlingPanel = createChunkHandlingPanel();
            add(chunkHandlingPanel, gbc);
        }
    }

    private JPanel createChunkHandlingPanel() {
        JPanel result = new JPanel(new GridBagLayout());
        result.setBorder(BorderFactory.createTitledBorder(
                " Chunk Size Handling "));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 20, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        result.add(m_chunkButtonEntireTable, gbc);

        gbc.gridy += 1;
        result.add(m_chunkButtonSingleRow, gbc);

        gbc.gridy += 1;
        result.add(m_chunkButtonChunkSize, gbc);

        gbc.gridy += 1;
        result.add(m_chunkButtonNrChunks, gbc);

        gbc.gridx += 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        result.add(m_chunkSizeSpinner, gbc);

        gbc.gridy += 1;
        result.add(m_nrChunksSpinner, gbc);

        return result;
    }

    /** Restores settings from a settings object, inits defaults if that fails.
     * @param settings To load from.
     * @param inputSpecs The input table specs.
     * @throws NotConfigurableException If no valid configuration is possible.
     */
    protected void loadSettingsFrom(final ExttoolSettings settings,
            final DataTableSpec[] inputSpecs) throws NotConfigurableException {
        m_executablePanel.updateHistory();
        String pathToExecutable = settings.getPathToExecutable();
        m_executablePanel.setSelectedFile(pathToExecutable);
        Chunking chunking = settings.getChunking();
        int chunkValue = settings.getChunkValue();
        switch (chunking) {
        case EntireTable:
            m_chunkButtonEntireTable.doClick();
            break;
        case IndividualRow:
            m_chunkButtonSingleRow.doClick();
            break;
        case ChunksOfSize:
            m_chunkButtonChunkSize.doClick();
            m_chunkSizeSpinner.setValue(chunkValue);
            break;
        case NrChunks:
            m_chunkButtonNrChunks.doClick();
            m_nrChunksSpinner.setValue(chunkValue);
            break;
        default:
            NodeLogger.getLogger(getClass()).coding(
                    "Unknown chunking: " + chunking);
            m_chunkButtonEntireTable.doClick();
        }

        AbstractCommandlineSettings cmdSets = settings.getCommandlineSettings();
        m_commandlineControl.loadSettings(cmdSets, inputSpecs);
    }

    /** Saves the current configuration to the argument.
     * @param settings To save to.
     * @throws InvalidSettingsException If the current configuration is invalid.
     */
    protected void saveSettingsTo(final ExttoolSettings settings)
        throws InvalidSettingsException {
        String pathToExecutable = m_executablePanel.getSelectedFile();
        settings.setPathToExecutable(pathToExecutable);
        Chunking chunking;
        int chunkValue;

        if (m_chunkButtonChunkSize.isSelected()) {
            chunking = Chunking.ChunksOfSize;
            chunkValue = (Integer)m_chunkSizeSpinner.getValue();
        } else if (m_chunkButtonNrChunks.isSelected()) {
            chunking = Chunking.NrChunks;
            chunkValue = (Integer)m_nrChunksSpinner.getValue();
        } else if (m_chunkButtonSingleRow.isSelected()) {
            chunking = Chunking.IndividualRow;
            chunkValue = -1;
        } else { // m_chunkButtonEntireTable selected
            chunking = Chunking.EntireTable;
            chunkValue = -1;
        }
        settings.setChunking(chunking, chunkValue);
        AbstractCommandlineSettings cmdSets = settings.getCommandlineSettings();
//        m_commandlineControl.saveGlobalSettingsGlobal(settings);
        m_commandlineControl.saveSettings(cmdSets);
    }

    /** Test method for this panel.
     * @param args Ignored. */
    public static void main(final String[] args) {
        JFrame frame = new JFrame("Test " + ExternalToolPanel.class);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ExternalToolPanel panel = new ExternalToolPanel(
                new ExttoolCustomizer());
        panel.initLayout();
        frame.getContentPane().add(panel);
        frame.pack();
        frame.setVisible(true);
    }

}
