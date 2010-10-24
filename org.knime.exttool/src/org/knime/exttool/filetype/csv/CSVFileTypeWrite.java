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
 *   Mar 8, 2010 (wiswedel): created
 */
package org.knime.exttool.filetype.csv;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.base.node.io.csvwriter.CSVWriter;
import org.knime.base.node.io.csvwriter.FileWriterSettings;
import org.knime.base.node.io.csvwriter.FileWriterSettings.quoteMode;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.exttool.filetype.AbstractFileTypeWrite;
import org.knime.exttool.filetype.AbstractFileTypeWriteConfig;

/**
 * CSV write support.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
class CSVFileTypeWrite extends AbstractFileTypeWrite {

    private CSVFileTypeWriteConfig m_csvConfig;

    /** Create instance, associating it with its factory.
     * @param factory Factory that creates this instance.
     */
    public CSVFileTypeWrite(final CSVFileTypeFactory factory) {
        super(factory);
    }

    /** {@inheritDoc} */
    @Override
    public void prepare(final AbstractFileTypeWriteConfig config) {
        m_csvConfig = (CSVFileTypeWriteConfig)config;
    }

    /** {@inheritDoc} */
    @Override
    public void validateInput(final DataTableSpec spec)
            throws InvalidSettingsException {
        if (m_csvConfig.isIncludeAllColumns()) {
            // find at least one matching column
            boolean foundMatch = false;
            for (DataColumnSpec col : spec) {
                if (CSVFileTypeWriteConfig.COLUMN_FILTER.includeColumn(col)) {
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch) {
                throw new InvalidSettingsException(
                        "No approriate columns for CSV format");
            }
        } else {
            String[] includeColumns = m_csvConfig.getIncludeColumns();
            if (includeColumns == null) {
                throw new InvalidSettingsException(
                        "No columns for CSV input selected");
            }
            for (String s : includeColumns) {
                DataColumnSpec col = spec.getColumnSpec(s);
                if (col == null) {
                    throw new InvalidSettingsException(
                            "No such column in input: " + s);
                }
                if (!CSVFileTypeWriteConfig.COLUMN_FILTER.includeColumn(col)) {
                    throw new InvalidSettingsException("Inappropriate input "
                            + "column for CSV input table: " + col);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void writeTable(final DataTableSpec spec, final RowIterator it,
            final int rowCount, final OutputStream out,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        FileWriterSettings fileWriterSettings = new FileWriterSettings();
        fileWriterSettings.setWriteRowID(true);
        fileWriterSettings.setWriteColumnHeader(m_csvConfig.isWriteColHeader());
        fileWriterSettings.setColSeparator(m_csvConfig.getColDelimiter());
        fileWriterSettings.setQuoteMode(quoteMode.STRINGS);
        fileWriterSettings.setQuoteBegin(m_csvConfig.getQuoteChar());
        fileWriterSettings.setQuoteEnd(m_csvConfig.getQuoteChar());

        CSVWriter csvWriter = new CSVWriter(
                new OutputStreamWriter(out), fileWriterSettings);
        DataTable table = new DataTable() {
            private boolean m_firstIteration = true;
           /** {@inheritDoc} */
            @Override
            public DataTableSpec getDataTableSpec() {
                return spec;
            }
            /** {@inheritDoc} */
            @Override
            public RowIterator iterator() {
                if (!m_firstIteration) {
                    throw new IllegalStateException(
                            "Iterator must not be called multiple times");
                }
                m_firstIteration = false;
                return it;
            }
        };
        String[] includeCols;
        if (m_csvConfig.isIncludeAllColumns()) {
            List<String> includeColsL = new ArrayList<String>();
            for (DataColumnSpec col : spec) {
                if (CSVFileTypeWriteConfig.COLUMN_FILTER.includeColumn(col)) {
                    includeColsL.add(col.getName());
                }
            }
            includeCols = includeColsL.toArray(new String[includeColsL.size()]);
        } else {
            includeCols = m_csvConfig.getIncludeColumns();
        }
        table = new FilterColumnTable(table, includeCols);
        csvWriter.write(table, exec);
        csvWriter.close();
    }

}
