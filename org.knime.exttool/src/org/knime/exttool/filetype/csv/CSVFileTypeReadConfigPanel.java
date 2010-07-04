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
 *   Apr 8, 2010 (wiswedel): created
 */
package org.knime.exttool.filetype.csv;

import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.node.InvalidSettingsException;
import org.knime.exttool.filetype.AbstractFileTypeReadConfig;
import org.knime.exttool.filetype.AbstractFileTypeReadConfigPanel;

/** Configuration panel to CSV input files. It has controls for column header
 * (flag), row and column delimiters and quote characters.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
@SuppressWarnings("serial")
final class CSVFileTypeReadConfigPanel extends AbstractFileTypeReadConfigPanel {

    private final JCheckBox m_hasColHeaderChecker;
    private final JTextField m_colDelimField;
    private final JTextField m_rowDelimField;
    private final JTextField m_quoteCharField;

    /** Inits layout. */
    public CSVFileTypeReadConfigPanel() {
        super(new GridLayout(0, 2));
        m_hasColHeaderChecker = new JCheckBox("Has Column Header");
        int textFieldWidth = 3;
        m_colDelimField = new JTextField(textFieldWidth);
        m_colDelimField.setToolTipText("Use '\\t' for tab character");
        m_quoteCharField = new JTextField(textFieldWidth);
        m_rowDelimField = new JTextField(textFieldWidth);
        m_colDelimField.setToolTipText("Use '\\n' for newline character");

        add(getInFlowLayout(m_colDelimField, new JLabel("Column Delimiter")));
        add(getInFlowLayout(m_rowDelimField, new JLabel("Row Delimiter")));
        add(getInFlowLayout(m_quoteCharField, new JLabel("Quote Character")));
        add(getInFlowLayout(m_hasColHeaderChecker));
    }

    /** {@inheritDoc} */
    @Override
    public void loadSettings(final AbstractFileTypeReadConfig config) {
        CSVFileTypeReadConfig csvConfig = (CSVFileTypeReadConfig)config;
        m_colDelimField.setText(escape(csvConfig.getColDelimiter()));
        m_rowDelimField.setText(escape(csvConfig.getRowDelimiter()));
        m_quoteCharField.setText(csvConfig.getQuoteChar());
        m_hasColHeaderChecker.setSelected(csvConfig.hasColHeader());
    }

    /** {@inheritDoc} */
    @Override
    public void saveSettings(final AbstractFileTypeReadConfig config)
            throws InvalidSettingsException {
        CSVFileTypeReadConfig csvConfig = (CSVFileTypeReadConfig)config;
        csvConfig.setColDelimiter(unescape(m_colDelimField.getText()));
        csvConfig.setRowDelimiter(unescape(m_rowDelimField.getText()));
        String quoteChar = m_quoteCharField.getText();
        quoteChar = "".equals(quoteChar) ? null : quoteChar;
        csvConfig.setQuoteChar(quoteChar);
        csvConfig.setHasColHeader(m_hasColHeaderChecker.isSelected());
    }

    /** Converts '\\t' to '\t' (tab char) etc.
     * @param s The string to unescape
     * @return The unescaped string.  */
    static String unescape(final String s) {
        if ("\\t".equals(s)) {
            return "\t";
        } else if ("\\n".equals(s)) {
            return "\n";
        } else if ("\\r\\n".equals(s)) {
            return "\r\n";
        } else {
            return s;
        }
    }

    /** Converts '\t' (tab char) to '\\t' etc.
    * @param s The string to escape
    * @return The escaped string.  */
    static String escape(final String s) {
        if ("\t".equals(s)) {
            return "\\t";
        } else if ("\n".equals(s)) {
            return "\\n";
        } else if ("\r\n".equals(s)) {
            return "\\r\\n";
        } else {
            return s;
        }
    }

    /** Put all argument components into a new panel with flow layout.
     * @param comps Components to add.
     * @return A new flow layout panel containing all components.
     */
    static final JPanel getInFlowLayout(final JComponent... comps) {
        JPanel result = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JComponent c : comps) {
            result.add(c);
        }
        return result;
    }
}
