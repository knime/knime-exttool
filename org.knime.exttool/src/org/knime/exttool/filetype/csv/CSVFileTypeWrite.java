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

import org.knime.base.node.io.csvwriter.CSVWriter;
import org.knime.base.node.io.csvwriter.FileWriterSettings;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.exttool.filetype.AbstractFileTypeWrite;

/**
 * CSV write support.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
class CSVFileTypeWrite extends AbstractFileTypeWrite {

    private FileWriterSettings m_settings;

    /** Create instance, associating it with its factory.
     * @param factory Factory that creates this instance.
     */
    public CSVFileTypeWrite(final CSVFileTypeFactory factory) {
        super(factory);
        m_settings = new FileWriterSettings();
        m_settings.setWriteColumnHeader(true);
        m_settings.setWriteRowID(true);
        m_settings.setColSeparator(",");
    }

    /** {@inheritDoc} */
    @Override
    public void setSelectedInput(final DataColumnSpec... spec)
            throws InvalidSettingsException {
        // write everything that comes in.
    }

    /** {@inheritDoc} */
    @Override
    public void writeTable(final DataTableSpec spec, final RowIterator it,
            final int rowCount, final OutputStream out,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        CSVWriter csvWriter = new CSVWriter(
                new OutputStreamWriter(out), m_settings);
        DataTable table = new DataTable() {
            private boolean m_firstIteration = true;
           /** {@inheritDoc} */
            public DataTableSpec getDataTableSpec() {
                return spec;
            }
            /** {@inheritDoc} */
            public RowIterator iterator() {
                if (!m_firstIteration) {
                    throw new IllegalStateException(
                            "Iterator must not be called multiple times");
                }
                m_firstIteration = false;
                return it;
            }
        };
        csvWriter.write(table, exec);
        csvWriter.close();
    }

    /** {@inheritDoc} */
    @Override
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings = new FileWriterSettings(settings);
    }

    /** {@inheritDoc} */
    @Override
    public void saveSettings(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.saveSettingsTo(settings);
    }

}
