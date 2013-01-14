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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   21.08.2008 (thor): created
 */
package org.knime.exttool.chem.filetype.sdf;

import java.io.File;

import org.knime.chem.base.node.io.sdf.DefaultSDFWriter;
import org.knime.chem.base.node.io.sdf.SDFWriterSettings;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;

/**
 * This is the model for the SDF writer node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class SDFWriter extends DefaultSDFWriter {
    /** Create new object, intializing settings with defaults. */
    public SDFWriter() {
        super(new SDFWriterSettings());
    }


    /** Performs a node's configure step.
     * @param inSpec The input table spec.
     * @throws InvalidSettingsException If settings/input are invalid.
     * @see org.knime.core.node.NodeModel
     */
    @Override
    public void configure(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        super.configure(inSpec);
        checkFileAccess();
    }

    /** Called by both configure and execute to check file existence. Depending
     * upon the {@link SDFWriterSettings#overwriteOK()} this will throw an
     * exception.
     * @throws InvalidSettingsException If the file to write to exists and
     * overwriting the file is not allowed.
     */
    protected void checkFileAccess() throws InvalidSettingsException {
        File f = new File(m_settings.fileName());
        if (f.exists()) {
            if (m_settings.overwriteOK()) {
                setWarningMessage("Output file '" + m_settings.fileName()
                        + "' already exists");
            } else {
                throw new InvalidSettingsException("File exists and can't be "
                        + "overwritten, check dialog settings");
            }
        }
    }
}
