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
 *   Apr 13, 2010 (wiswedel): created
 */
package org.knime.exttool.filetype;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilter;

/**
 * Default write (input) config for external tool. It has only a single target
 * column field as parameter.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class DefaultFileTypeWriteConfig extends AbstractFileTypeWriteConfig {

    private final ColumnFilter m_columnFilter;

    private String m_column;

    /** Create new write config using a specified column filter to select
     * the appropriate columns in the dialog. Use
     * {@link org.knime.core.node.util.DataValueColumnFilter} to have a class
     * based column filter.
     * @param columnFilter The filter to use, must not be null.
     */
    public DefaultFileTypeWriteConfig(final ColumnFilter columnFilter) {
        if (columnFilter == null) {
            throw new NullPointerException("Argument must not be null");
        }
        m_columnFilter = columnFilter;
    }

    /** {@inheritDoc} */
    @Override
    public AbstractFileTypeWriteConfigPanel createConfigPanel() {
        return new DefaultFileTypeWriteConfigPanel(m_columnFilter);
    }

    /** Config key used to save the selected column in the load/save methods. */
    static final String CFG_COLUMN = "column";

    /** {@inheritDoc} */
    @Override
    public void loadSettingsInDialog(
            final NodeSettingsRO settings, final DataTableSpec spec)
            throws NotConfigurableException {
        String column = settings.getString(CFG_COLUMN, null);
        DataColumnSpec col = spec.getColumnSpec(column);
        if (col != null && m_columnFilter.includeColumn(col)) {
            // accept as is
        } else {
            column = null;
            for (DataColumnSpec c : spec) {
                if (m_columnFilter.includeColumn(c)) {
                    column = c.getName();
                    break;
                }
            }
        }
        if (column == null) {
            throw new NotConfigurableException("No column of expected "
                    + "type in input table");
        }
        m_column = column;
    }

    /** {@inheritDoc} */
    @Override
    public void loadSettingsInModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String column = settings.getString(CFG_COLUMN);
        if (column == null || column.length() == 0) {
            throw new InvalidSettingsException(
                    "Invalid column name: " + column);
        }
        m_column = column;
    }

    /** {@inheritDoc} */
    @Override
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString(CFG_COLUMN, m_column);
    }

    /**
     * @return the column
     */
    public String getColumn() {
        return m_column;
    }

    /**
     * @param column the column to set
     */
    public void setColumn(final String column) {
        m_column = column;
    }

}
