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

import java.util.Arrays;

import org.knime.core.data.DataValue;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.util.DataValueColumnFilter;

/**
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class ExttoolCustomizer {


    private int m_nrInputs = 1;
    private int m_nrOutputs = 1;
    private boolean m_showChunkSizeHandlingPanel = true;
    private boolean m_showPathToExecutableField = false;
    private boolean m_showPathToTempInputFile = true;
    private boolean m_showPathToTempOutputFile = true;
    private boolean m_showOutputTypeSelection = true;
    private boolean m_showTargetColumnCombo = true;
    private boolean m_showTabInputFile = true;
    private boolean m_showTabOutputFile = true;

    private String m_executableFileHistoryID = "exttool";
    private String[] m_executableFileHistorySuffixes = new String[0];

    @SuppressWarnings("unchecked")
    private ColumnFilter m_columnFilter =
        new DataValueColumnFilter(DataValue.class);

    protected AbstractCommandlineSettings createCommandlineSettings() {
        return new FreeFormCommandlineSettings();
    }

    protected ExttoolSettings createExttoolSettings() {
        return new ExttoolSettings(this);
    }

    protected Execution createExecution(final ExttoolSettings settings) {
        return new Execution(this, settings);
    }

    /**
     * @return the nrInputs
     */
    public int getNrInputs() {
        return m_nrInputs;
    }

    /**
     * @param inputs the nrInputs to set
     */
    public void setNrInputs(final int inputs) {
        if (inputs < 0) {
            throw new IllegalArgumentException("Illegal port count: " + inputs);
        }
        m_nrInputs = inputs;
    }

    /**
     * @return the nrOutputs
     */
    public int getNrOutputs() {
        return m_nrOutputs;
    }

    /**
     * @param outs the nrOutputs to set
     */
    public void setNrOutputs(final int outs) {
        if (outs < 0) {
            throw new IllegalArgumentException("Illegal port count: " + outs);
        }
        m_nrOutputs = outs;
    }

    /**
     * @return the showChunkSizeHandlingPanel
     */
    public boolean isShowChunkSizeHandlingPanel() {
        return m_showChunkSizeHandlingPanel;
    }

    /**
     * @param showChunkSizeHandlingPanel the showChunkSizeHandlingPanel to set
     */
    public void setShowChunkSizeHandlingPanel(
            final boolean showChunkSizeHandlingPanel) {
        m_showChunkSizeHandlingPanel = showChunkSizeHandlingPanel;
    }

    /**
     * @return the showPathToExecutableField
     */
    public boolean isShowPathToExecutableField() {
        return m_showPathToExecutableField;
    }

    /**
     * @param showPathToExecutableField the showPathToExecutableField to set
     */
    public void setShowPathToExecutableField(
            final boolean showPathToExecutableField) {
        m_showPathToExecutableField = showPathToExecutableField;
    }

    /**
     * @return the showPathTempInputFile
     */
    public boolean isShowPathToTempInputFile() {
        return m_showPathToTempInputFile;
    }

    /**
     * @param showPathToTempInputFile the showPathTempInputFile to set
     */
    public void setShowPathToTempInputFile(
            final boolean showPathToTempInputFile) {
        m_showPathToTempInputFile = showPathToTempInputFile;
    }

    /**
     * @return the showPathTempOutputFile
     */
    public boolean isShowPathToTempOutputFile() {
        return m_showPathToTempOutputFile;
    }

    /**
     * @param showPathToTempOutputFile the showPathTempOutputFile to set
     */
    public void setShowPathToTempOutputFile(
            final boolean showPathToTempOutputFile) {
        m_showPathToTempOutputFile = showPathToTempOutputFile;
    }

    /**
     * @param showOutputTypeSelection the showOutputTypeSelection to set
     */
    public void setShowOutputTypeSelection(
            final boolean showOutputTypeSelection) {
        m_showOutputTypeSelection = showOutputTypeSelection;
    }

    /**
     * @return the showOutputTypeSelection
     */
    public boolean isShowOutputTypeSelection() {
        return m_showOutputTypeSelection;
    }

    /**
     * @return the showTargetColumnCombo
     */
    public boolean isShowTargetColumnCombo() {
        return m_showTargetColumnCombo;
    }

    /**
     * @param showTargetColumnCombo the showTargetColumnCombo to set
     */
    public void setShowTargetColumnCombo(final boolean showTargetColumnCombo) {
        m_showTargetColumnCombo = showTargetColumnCombo;
    }

    /**
     * @return the showTabInputFile
     */
    public boolean isShowTabInputFile() {
        return m_showTabInputFile;
    }

    /**
     * @param showTabInputFile the showTabInputFile to set
     */
    public void setShowTabInputFile(final boolean showTabInputFile) {
        m_showTabInputFile = showTabInputFile;
    }

    /**
     * @return the showTabOutputFile
     */
    public boolean isShowTabOutputFile() {
        return m_showTabOutputFile;
    }

    /**
     * @param showTabOutputFile the showTabOutputFile to set
     */
    public void setShowTabOutputFile(final boolean showTabOutputFile) {
        m_showTabOutputFile = showTabOutputFile;
    }

    /**
     * @return the executableFileHistoryID
     */
    public String getExecutableFileHistoryID() {
        return m_executableFileHistoryID;
    }

    /**
     * @param executableFileHistoryID the executableFileHistoryID to set
     */
    public void setExecutableFileHistoryID(
            final String executableFileHistoryID) {
        if (executableFileHistoryID == null) {
            throw new NullPointerException("Argument must not be null");
        }
        if (m_executableFileHistoryID.trim().length() == 0) {
            throw new IllegalArgumentException("Illegal history id: \""
                    + m_executableFileHistoryID + "\"");
        }
        m_executableFileHistoryID = executableFileHistoryID;
    }

    /**
     * @return the executableFileHistorySuffixes
     */
    public String[] getExecutableFileHistorySuffixes() {
        return m_executableFileHistorySuffixes;
    }

    /**
     * @param suffixes the executableFileHistorySuffixes to set
     */
    public void setExecutableFileHistorySuffixes(
            final String... suffixes) {
        if (suffixes == null || Arrays.asList(suffixes).contains(null)) {
            throw new NullPointerException("Argument must not be null or "
                    + "contain null elements");
        }
        m_executableFileHistorySuffixes = suffixes;
    }

    /** @param columnFilter the columnFilter to set */
    public void setColumnFilter(final ColumnFilter columnFilter) {
        if (columnFilter == null) {
            throw new NullPointerException("Argument must not be null");
        }
        m_columnFilter = columnFilter;
    }

    /**
     * @param valClass A class of DataValue class that are accepted
     * @see #setColumnFilter(ColumnFilter)
     * @see DataValueColumnFilter
     * @throws NullPointerException If the argument is <code>null</code> or
     * contains <code>null</code> arguments.
     */
    public void setColumnFilter(final Class<? extends DataValue>... valClass) {
        setColumnFilter(new DataValueColumnFilter(valClass));
    }

    /** @return the columnFilter */
    public ColumnFilter getColumnFilter() {
        return m_columnFilter;
    }

}
