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
 *   Mar 6, 2010 (wiswedel): created
 */
package org.knime.exttool.chem.filetype.mol2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.exttool.executor.OutputDataHandle;
import org.knime.exttool.filetype.AbstractFileTypeRead;
import org.knime.exttool.filetype.AbstractFileTypeReadConfig;

/**
 * Mol2 read support.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class Mol2FileTypeRead extends AbstractFileTypeRead {

    /** Create new reader from a given factory (for meta information).
     * @param factory passed to super class, must not be nul.
     */
    Mol2FileTypeRead(final Mol2FileTypeFactory factory) {
        super(factory);
    }

    /** {@inheritDoc} */
    @Override
    public void prepare(final AbstractFileTypeReadConfig config) {
        // no op
    }

    /** {@inheritDoc} */
    @Override
    public BufferedDataTable readTable(final OutputDataHandle in,
            final ExecutionContext exec) throws Exception {
        Mol2Reader reader = new Mol2Reader() {

            private boolean m_firstInstance = true;
            /** {@inheritDoc} */
            @Override
            protected void checkSourceAccess() throws InvalidSettingsException {
                // ignored, always valid input
            }

            /** {@inheritDoc} */
            @Override
            protected BufferedReader openReader() throws IOException {
                if (!m_firstInstance) {
                    throw new IllegalStateException(
                            "Must not open reader multiple times");
                }
                return new BufferedReader(new InputStreamReader(
                        in.openOutputFileInStream()));
            }
        };
        reader.setExtractMolName(true);
        reader.setGenerateID(true);
        BufferedDataTable result = reader.execute(exec);
        ColumnRearranger rearranger = new ColumnRearranger(
                result.getDataTableSpec());
        rearranger.move(Mol2Reader.MOLECULE_COLNAME_SPEC.getName(), 0);
        return exec.createColumnRearrangeTable(result, rearranger, exec);
    }

}
