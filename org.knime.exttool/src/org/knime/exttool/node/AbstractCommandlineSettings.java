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
 *   Jan 20, 2010 (wiswedel): created
 */
package org.knime.exttool.node;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/** Command line settings represent the command line that is eventually
 * executed by the external tool node. In the simplest case this is just a
 * string with different switches (which is also the default in the standard
 * external tool node). This can be more complicated, for instance if the
 * corresponding dialog has combo boxes or textfields for different arguments.
 *
 * <p>
 * Extensions of this class are used to
 * <ul>
 * <li>Save/load the settings (toggle states, content of text fields, etc...)
 * <li>Create the command line arguments that are passed to the external process
 * </ul>
 * Objects of this class are not used to handle the GUI elements.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public abstract class AbstractCommandlineSettings {

    /** Allows sub-classes to modify the settings that are part of the
     * {@link ExttoolSettings} configuration, such as in- and output file
     * type configuration. This is necessary if custom node implementations do
     * not use the in- and output panels but decide to do this type of
     * configuration in their own control panel.
     *
     * <p>This method is called shortly before save and only if this settings
     * object is used in a dialog context (not in the model).
     *
     * <p>Settings that are specific to sub-classes should be saved in
     * the {@link #saveSettings(NodeSettingsWO)} method.
     *
     * @param exttoolSettings To be used to overwrite the settings that
     * are contributed by the standard components such as in- and output file
     * type configuration. */
    protected abstract void correctSettingsForSave(final ExttoolSettings
            exttoolSettings);

    /** Saves the current state to the argument.
     * @param settings To save any configuration that is special to this
     * implementation of a command line settings and which can't be persisted
     * in the {@link ExttoolSettings} argument.
     * @see #correctSettingsForSave(ExttoolSettings) */
    protected abstract void saveSettings(final NodeSettingsWO settings);

    /** Load a state from a settings object. This method is called from the
     * load method in the {@link ExttoolNodeModel}.
     * @param exttoolSettings To read type specific configuration from.
     * @param settings To load extra parameters from.
     * @throws InvalidSettingsException If settings are incomplete or invalid.
     */
    protected abstract void loadSettingsInModel(final ExttoolSettings
            exttoolSettings, final NodeSettingsRO settings)
        throws InvalidSettingsException;

    /** Load the settings in the dialog, falling back to default in case of
     * errors.
     * @param exttoolSettings To read standard parameters from, such as
     * in- and output file type configuration.
     * @param settings To load extra parameters from.
     * @param inSpecs The input table specs.
     * @throws NotConfigurableException If no valid configuration is possible.
     */
    protected abstract void loadSettingsInDialog(final ExttoolSettings
            exttoolSettings,  final NodeSettingsRO settings,
            final DataTableSpec[] inSpecs) throws NotConfigurableException;

    /** Get the command line arguments to the external process.
     * @return The array of the command line args, the different values may
     *         contain place holders %inFile% or %outFile% to represent the
     *         respective in/output.
     * @throws InvalidSettingsException If the settings are invalid.
     */
    protected abstract String[] getCommandlineArgs()
        throws InvalidSettingsException;

    /** Create a new controller that will put GUI elements into a panel.
     * The {@link AbstractCommandlineControl#saveSettings(
     * AbstractCommandlineSettings) load and save methods} of the returned
     * object can safely type-cast settings argument of this concrete subclass.
     * @return A new controller.
     */
    protected abstract AbstractCommandlineControl createControl();

}
