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

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.util.DataValueColumnFilter;
import org.knime.exttool.filetype.AbstractFileTypeWriteConfig;

/**
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class CSVFileTypeWriteConfig extends AbstractFileTypeWriteConfig {

    /** Column filter to only include int, double, String columns. */
    @SuppressWarnings("unchecked")
    public static final ColumnFilter COLUMN_FILTER = new DataValueColumnFilter(
            StringValue.class, DoubleValue.class, IntValue.class);

    private boolean m_writeColHeader;
    private String m_colDelimiter;
    private String m_quoteChar;
    private String[] m_includeColumns;
    private boolean m_includeAllColumns;

    /**
     * @return the writeColHeader
     */
    public boolean isWriteColHeader() {
        return m_writeColHeader;
    }

    /**
     * @param writeColHeader the writeColHeader to set
     */
    public void setWriteColHeader(final boolean writeColHeader) {
        m_writeColHeader = writeColHeader;
    }

    /**
     * @return the colDelimiter
     */
    public String getColDelimiter() {
        return m_colDelimiter;
    }

    /**
     * @param colDelimiter the colDelimiter to set
     */
    public void setColDelimiter(final String colDelimiter) {
        m_colDelimiter = colDelimiter;
    }

    /**
     * @return the quoteChar
     */
    public String getQuoteChar() {
        return m_quoteChar;
    }

    /**
     * @param quoteChar the quoteChar to set
     */
    public void setQuoteChar(final String quoteChar) {
        m_quoteChar = quoteChar;
    }

    /**
     * @return the includeColumns
     */
    public String[] getIncludeColumns() {
        return m_includeColumns;
    }

    /**
     * @param includeColumns the includeColumns to set
     */
    public void setIncludeColumns(final String[] includeColumns) {
        m_includeColumns = includeColumns;
    }

    /**
     * @return the includeAllColumns
     */
    public boolean isIncludeAllColumns() {
        return m_includeAllColumns;
    }

    /**
     * @param includeAllColumns the includeAllColumns to set
     */
    public void setIncludeAllColumns(final boolean includeAllColumns) {
        m_includeAllColumns = includeAllColumns;
    }

    /** {@inheritDoc} */
    @Override
    public CSVFileTypeWriteConfigPanel createConfigPanel() {
        return new CSVFileTypeWriteConfigPanel();
    }

    /** {@inheritDoc} */
    @Override
    public void loadSettingsInDialog(final NodeSettingsRO settings,
            final DataTableSpec spec) throws NotConfigurableException {
        m_writeColHeader = settings.getBoolean("writeColHeader", true);
        m_colDelimiter = settings.getString("colDelimiter", ",");
        m_quoteChar = settings.getString("quoteChar", "\"");
        m_includeAllColumns = settings.getBoolean("includeAllColumns", true);
        List<String> defIncludes = new ArrayList<String>();
        for (DataColumnSpec col : spec) {
            if (COLUMN_FILTER.includeColumn(col)) {
                defIncludes.add(col.getName());
            }
        }
        m_includeColumns = settings.getStringArray("includeColumns",
                defIncludes.toArray(new String[defIncludes.size()]));
    }

    /** {@inheritDoc} */
    @Override
    public void loadSettingsInModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_writeColHeader = settings.getBoolean("writeColHeader");
        m_colDelimiter = settings.getString("colDelimiter");
        if (m_colDelimiter == null || m_colDelimiter.length() < 1) {
            throw new InvalidSettingsException("Invalid column delimiter \""
                    + m_colDelimiter + "\"");
        }
        m_quoteChar = settings.getString("quoteChar");
        if ("".equals(m_quoteChar)) {
            m_quoteChar = null;
        }
        if (m_quoteChar != null) {
            if (m_quoteChar.length() != 1) {
                throw new InvalidSettingsException("Invalid quote char '"
                        + m_colDelimiter + "'");
            }
            if (m_colDelimiter.contains(m_quoteChar)) {
                throw new InvalidSettingsException("Column delimiter (\""
                        + m_colDelimiter + "\") must not contain quote char ('"
                        + m_quoteChar + "')");
            }
        }
        m_includeAllColumns = settings.getBoolean("includeAllColumns");
        if (!m_includeAllColumns) {
            m_includeColumns = settings.getStringArray("includeColumns");
        } else {
            m_includeColumns = new String[0];
        }
    }

    /** {@inheritDoc} */
    @Override
    public void saveSettings(final NodeSettingsWO settings) {
        if (m_colDelimiter == null) {
            // no valid config.
            return;
        }
        settings.addBoolean("writeColHeader", m_writeColHeader);
        settings.addString("colDelimiter", m_colDelimiter);
        settings.addString("quoteChar", m_quoteChar);
        settings.addBoolean("includeAllColumns", m_includeAllColumns);
        if (!m_includeAllColumns) {
            settings.addStringArray("includeColumns", m_includeColumns);
        }
    }

}
