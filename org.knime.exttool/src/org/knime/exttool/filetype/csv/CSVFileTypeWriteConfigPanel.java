/*
 * ------------------------------------------------------------------------
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Apr 8, 2010 (wiswedel): created
 */
package org.knime.exttool.filetype.csv;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilterPanel;
import org.knime.exttool.filetype.AbstractFileTypeWriteConfig;
import org.knime.exttool.filetype.AbstractFileTypeWriteConfigPanel;

/** Configuration panel for CSV input format (CSV writing). It contains
 * parameters for column header inclusion, column delimiter, quote chars and
 * a list of columns to include.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
@SuppressWarnings("serial")
final class CSVFileTypeWriteConfigPanel extends
        AbstractFileTypeWriteConfigPanel {

    private final JCheckBox m_writeColHeaderChecker;
    private final JTextField m_colDelimField;
    private final JTextField m_quoteCharField;
    private final ColumnFilterPanel m_columnFilterPanel;

    /** Inits layout. */
    CSVFileTypeWriteConfigPanel() {
        super(new BorderLayout());
        m_writeColHeaderChecker = new JCheckBox("Write Column Header");
        int textFieldWidth = 3;

        m_colDelimField = new JTextField(textFieldWidth);
        m_colDelimField.setToolTipText("Use '\\t' for tab character");
        m_quoteCharField = new JTextField(textFieldWidth);
        m_columnFilterPanel = new ColumnFilterPanel(
                true, CSVFileTypeWriteConfig.COLUMN_FILTER);

        JPanel northPanel = new JPanel(new GridLayout(0, 2));
        northPanel.add(CSVFileTypeReadConfigPanel.getInFlowLayout(
                m_colDelimField, new JLabel("Column Delimiter")));
        northPanel.add(CSVFileTypeReadConfigPanel.getInFlowLayout(
                m_quoteCharField, new JLabel("Quote Character")));
        northPanel.add(CSVFileTypeReadConfigPanel.getInFlowLayout(
                m_writeColHeaderChecker));
        northPanel.add(CSVFileTypeReadConfigPanel.getInFlowLayout(
                new JLabel()));
        add(northPanel, BorderLayout.NORTH);

        m_columnFilterPanel.setBorder(BorderFactory.createTitledBorder(
                " Columns to write "));
        add(m_columnFilterPanel, BorderLayout.CENTER);
    }

    /** {@inheritDoc} */
    @Override
    public void loadSettings(final AbstractFileTypeWriteConfig config,
            final DataTableSpec spec) throws NotConfigurableException {
        CSVFileTypeWriteConfig csvConfig = (CSVFileTypeWriteConfig)config;
        m_writeColHeaderChecker.setSelected(csvConfig.isWriteColHeader());
        m_colDelimField.setText(CSVFileTypeReadConfigPanel.escape(
                csvConfig.getColDelimiter()));
        m_quoteCharField.setText(CSVFileTypeReadConfigPanel.escape(
                csvConfig.getQuoteChar()));
        String[] incls = csvConfig.getIncludeColumns();
        @SuppressWarnings("unchecked")
        List<String> inclList = incls == null ? Collections.EMPTY_LIST
                : Arrays.asList(incls);
        m_columnFilterPanel.setKeepAllSelected(csvConfig.isIncludeAllColumns());
        m_columnFilterPanel.update(spec, false, inclList);
    }

    /** {@inheritDoc} */
    @Override
    public void saveSettings(final AbstractFileTypeWriteConfig config)
            throws InvalidSettingsException {
        CSVFileTypeWriteConfig csvConfig = (CSVFileTypeWriteConfig)config;
        csvConfig.setWriteColHeader(m_writeColHeaderChecker.isSelected());
        csvConfig.setColDelimiter(CSVFileTypeReadConfigPanel.unescape(
                m_colDelimField.getText()));
        String quoteChar = m_quoteCharField.getText();
        quoteChar = "".equals(quoteChar) ? null : quoteChar;
        csvConfig.setQuoteChar(CSVFileTypeReadConfigPanel.unescape(quoteChar));
        csvConfig.setIncludeAllColumns(m_columnFilterPanel.isKeepAllSelected());
        Set<String> includes = m_columnFilterPanel.getIncludedColumnSet();
        String[] toArray = includes.toArray(new String[includes.size()]);
        csvConfig.setIncludeColumns(toArray);
    }

}
