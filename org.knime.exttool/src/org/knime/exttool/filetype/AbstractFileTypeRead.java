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
package org.knime.exttool.filetype;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.exttool.executor.OutputDataHandle;


/** File type supporting reading files (output of the external tool).
 *
 * <p><b>Warning:</b> API needs review, subclassing and usage outside this
 * package is currently not encouraged.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public abstract class AbstractFileTypeRead extends AbstractFileType {

    /** Create instance, associating it with its factory.
     * @param factory Factory that creates this instance.
     */
    protected AbstractFileTypeRead(final AbstractFileTypeFactory factory) {
        super(factory);
    }

    /** Load settings from reader configuration. The argument can be safely
     * type casted to the object that is returned by the associated factory's
     * {@link AbstractFileTypeFactory#createNewReadConfig()} method.
     * @param config To load from.
     */
    public abstract void prepare(AbstractFileTypeReadConfig config);

    /** Read the file from the given output data handle and returns a table
     * containing the content. The table <b>must</b> have as its first column
     * the IDs that are used to merge the output with the input data. It must be
     * a separate column since the output can contain multiple matches for an
     * input record. The implementation can safely assume that the
     * {@link #prepare(AbstractFileTypeReadConfig)} method has been called
     * beforehand.
     * @param handle the output handle.
     * @param exec for progress/cancelation/table creation
     * @return the output table
     * @throws Exception In case of problems
     */
    public abstract BufferedDataTable readTable(final OutputDataHandle handle,
            final ExecutionContext exec) throws Exception;

}
