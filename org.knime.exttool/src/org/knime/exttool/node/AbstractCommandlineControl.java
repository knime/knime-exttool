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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Mar 10, 2010 (wiswedel): created
 */
package org.knime.exttool.node;

import java.awt.GridBagConstraints;

import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;

/**
 * GUI controller for {@link AbstractCommandlineSettings}. Objects of this
 * class are created using the corresponding factory method
 * {@link AbstractCommandlineSettings#createControl()}.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public abstract class AbstractCommandlineControl {

    /** Called from {@link #registerPanel(JPanel, GridBagConstraints)} to
     * allow this control object to register a basic panel to the parent.
     * Subclasses can alternatively overwrite
     * {@link #registerPanel(JPanel, GridBagConstraints)} and beautify the
     * layout a bit (two column layout). This abstract method should be
     * implemented empty in this case.
     * @param parent The panel where to add own GUI elements.
     */
    protected abstract void registerPanel(final JPanel parent);

    /** Called from the framework to register custom GUI objects to the parent
     * panel. The <code>parent</code> has a {@link java.awt.GridBagLayout}
     * with two columns. In most cases it's easier to simply implement
     * the {@link #registerPanel(JPanel)} method only.
     * @param parent Where to register components
     * @param gbc The constraints to layout the parent.
     */
    protected void registerPanel(final JPanel parent,
            final GridBagConstraints gbc) {
        registerPanel(parent);
    }

    /** Load the settings from the associated command line settings. The
     * argument can be safely type-casted to the class that created this
     * control in its {@link AbstractCommandlineSettings#createControl()}
     * method.
     * @param settings To load from.
     * @param spec The input table specs
     * @throws NotConfigurableException If no valid configuration is possible.
     */
    protected abstract void loadSettings(
            final AbstractCommandlineSettings settings,
            final DataTableSpec[] spec) throws NotConfigurableException;

    /** Saves the settings to the associated command line settings. The
     * argument can be safely type-casted to the class that created this
     * control in its {@link AbstractCommandlineSettings#createControl()}
     * method.
     * @param settings To save to.
     * @throws InvalidSettingsException If the current configuration is invalid.
     */
    protected abstract void saveSettings(
            final AbstractCommandlineSettings settings)
        throws InvalidSettingsException;

}
