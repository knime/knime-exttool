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
 *   Jun 23, 2010 (wiswedel): created
 */
package org.knime.exttool.executor;

import java.awt.LayoutManager;

import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;

/** Dialog allowing configuration of an executor. Sub-classes are always bound
 * to their accompanying {@link AbstractExttoolExecutorConfig}.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("serial")
public abstract class AbstractExttoolExecutorConfigPanel extends JPanel {

    /** Inherited super constructor.
     * @see JPanel#JPanel() */
    public AbstractExttoolExecutorConfigPanel() {
        super();
    }

    /** Inherited super constructor.
     * @param layout The layout passed to super.
     * @see JPanel#JPanel(LayoutManager) */
    public AbstractExttoolExecutorConfigPanel(final LayoutManager layout) {
        super(layout);
    }

    /** Load from a config object. It can be safely type-casted to the expected
     * class (the class that created this instance).
     * @param config To load from.
     * @throws NotConfigurableException If not configurable for any reason.
     */
    public abstract void loadSettings(
            final AbstractExttoolExecutorConfig config)
    throws NotConfigurableException;

    /** Saves the current settings to argument config object.
     * @param config To save to.
     * @throws InvalidSettingsException If something is invalid.
     */
    public abstract void saveSettings(
            final AbstractExttoolExecutorConfig config)
        throws InvalidSettingsException;

}
