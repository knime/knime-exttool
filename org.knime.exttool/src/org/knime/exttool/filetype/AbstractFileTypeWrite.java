/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
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
 *   Jan 20, 2010 (wiswedel): created
 */
package org.knime.exttool.filetype;

import java.io.IOException;
import java.io.OutputStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;


/** File type supporting writing files (input to the external tool).
 *
 * <p><b>Warning:</b> API needs review, subclassing and usage outside this
 * package is currently not encouraged. Specifically, the write method needs
 * a careful review.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public abstract class AbstractFileTypeWrite extends AbstractFileType {

    /** Create instance, associating it with its factory.
     * @param factory Factory that creates this instance.
     */
    protected AbstractFileTypeWrite(final AbstractFileTypeFactory factory) {
        super(factory);
    }

    /** Load settings from writer configuration. The argument can be safely
     * type casted to the object that is returned by the associated factory's
     * {@link AbstractFileTypeFactory#createNewWriteConfig()} method.
     * @param config To load from.
     */
    public abstract void prepare(AbstractFileTypeWriteConfig config);

    /** Validates that the argument input spec can be processe with the current
     * settings. This method is called during the node configuration.  It can
     * safely be assumed that {@link #prepare(AbstractFileTypeWriteConfig)}
     * has been called beforehand.
     * @param spec The input spec, never null.
     * @throws InvalidSettingsException If the input can't be processed given
     *         the current settings.
     */
    public abstract void validateInput(
            final DataTableSpec spec) throws InvalidSettingsException;

    /** Write the input table to the output stream that comes from the
     * corresponding {@link org.knime.exttool.executor.InputDataHandle}. It can
     * safely be assumed that {@link #prepare(AbstractFileTypeWriteConfig)}
     * has been called beforehand.
     * @param spec The spec of the table.
     * @param it The iterator returning the data.
     * @param rowCount The row count in the iterator (for progress)
     * @param out The output stream to write to (must be closed afterwards)
     * @param exec For progress/cancelation.
     * @throws IOException In case of I/O problems.
     * @throws CanceledExecutionException If canceled.
     */
    public abstract void writeTable(DataTableSpec spec,
            final RowIterator it, int rowCount, final OutputStream out,
            final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException;

}
