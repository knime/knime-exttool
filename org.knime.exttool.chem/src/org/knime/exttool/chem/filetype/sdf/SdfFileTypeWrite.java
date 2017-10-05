/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 20, 2010 (wiswedel): created
 */
package org.knime.exttool.chem.filetype.sdf;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.knime.chem.base.node.io.sdfwriter2.DefaultSDFWriter;
import org.knime.chem.base.node.io.sdfwriter2.SDFWriterSettings;
import org.knime.chem.types.SdfValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.exttool.filetype.AbstractFileTypeWrite;
import org.knime.exttool.filetype.AbstractFileTypeWriteConfig;

/**
 * SDF write support.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class SdfFileTypeWrite extends AbstractFileTypeWrite {

    private SDFWriterSettings m_settings;

    /** Create new write instance.
     * @param factory Registered factory for this write object.
     */
    SdfFileTypeWrite(final SdfFileTypeFactory factory) {
        super(factory);
        m_settings = new SDFWriterSettings();
        m_settings.overwriteOK(true);
        m_settings.titleColumn(SDFWriterSettings.ROW_KEY_IDENTIFIER);
        m_settings.skipProblematicItems(true);
    }

    /** {@inheritDoc} */
    @Override
    public void prepare(final AbstractFileTypeWriteConfig config) {
        SdfFileTypeWriteConfig conf = (SdfFileTypeWriteConfig)config;
        String targetColumn = conf.getColumn();
        m_settings.structureColumn(targetColumn);
        m_settings.setFilterConfiguration(
            new DataColumnSpecFilterConfiguration("column-filter", new InputFilter<DataColumnSpec>() {
                @Override
                public boolean include(final DataColumnSpec colSpec) {
                    return conf.getPropertiesColumns().contains(colSpec.getName());
                }
            }));
    }

    /** {@inheritDoc} */
    @Override
    public void validateInput(final DataTableSpec spec)
            throws InvalidSettingsException {
        String targetColumn = m_settings.structureColumn();
        DataColumnSpec col = spec.getColumnSpec(targetColumn);
        if (col == null) {
            throw new InvalidSettingsException(
                    "No such column: " + targetColumn);
        }
        if (!col.getType().isCompatible(SdfValue.class)) {
            throw new InvalidSettingsException("Input column \""
                    + targetColumn + "\" is not SDF compatible");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void writeTable(final DataTableSpec spec, final RowIterator it,
            final int rowCount, final OutputStream out,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        DefaultSDFWriter writer = new DefaultSDFWriter(m_settings) {
            /** {@inheritDoc} */
            @Override
            protected BufferedWriter openOutputWriter()
                    throws InvalidSettingsException, IOException {
                return new BufferedWriter(new OutputStreamWriter(out));
            }
        };
        try {
            writer.execute(spec, it, rowCount, exec);
        } catch (CanceledExecutionException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(
                    "Unable to write SDF stream: " + e.getMessage(), e);
        }
    }

}

