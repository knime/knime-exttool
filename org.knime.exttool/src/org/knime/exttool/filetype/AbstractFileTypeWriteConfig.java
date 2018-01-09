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
 *   Apr 8, 2010 (wiswedel): created
 */
package org.knime.exttool.filetype;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/** Customization of the writer that creates the input to the external tool.
 * This can be, e.g. the list of columns to write, what type of delimiter is
 * used in CSV and so on.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public abstract class AbstractFileTypeWriteConfig {

    /** Loads settings, fails if invalid.
     * @param settings To load from.
     * @throws InvalidSettingsException If invalid.
     */
    public abstract void loadSettingsInModel(final NodeSettingsRO settings)
        throws InvalidSettingsException;

    /** Load settings, init with defaults if invalid.
     * @param settings To load from.
     * @param spec The input spec
     * @throws NotConfigurableException If the input is of unexpected type,
     *         e.g. does not contain a compatible type.
     */
    public abstract void loadSettingsInDialog(final NodeSettingsRO settings,
            final DataTableSpec spec) throws NotConfigurableException;

    /** Saves current settings.
     * @param settings To save to.
     */
    public abstract void saveSettings(final NodeSettingsWO settings);

    /** Create a new panel that will be displayed in the "input file" panel
     * of the external tool node. The returned object (not null) can safely
     * type cast the config object to this concrete class in its load/save
     * methods.
     * @return a new config panel.
     */
    public abstract AbstractFileTypeWriteConfigPanel createConfigPanel();
}
