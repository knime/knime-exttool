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
 *   Apr 8, 2010 (wiswedel): created
 */
package org.knime.exttool.filetype;

import java.awt.LayoutManager;

import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;

/**
 * A panel allowing the user to customize the writing of the input files.
 * It is the view to {@link AbstractFileTypeWriteConfig}.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
@SuppressWarnings("serial")
public abstract class AbstractFileTypeWriteConfigPanel extends JPanel {

    /** Delegating to super constructor, see {@link JPanel#JPanel()}. */
    public AbstractFileTypeWriteConfigPanel() {
        super();
    }

    /** Delegating to super constructor,
     * see {@link JPanel#JPanel(LayoutManager)}.
     * @param layout Passed to super constructor. */
    public AbstractFileTypeWriteConfigPanel(final LayoutManager layout) {
        super(layout);
    }

    /** Load settings into dialog. Argument can be safely type casted to
     * expected config class.
     * @param config To load from.
     * @param spec The input spec.
     * @throws NotConfigurableException If the input spec does not contain
     *         the expected type (required action if the associated file type
     *         should be disabled).
     */
    public abstract void loadSettings(final AbstractFileTypeWriteConfig config,
            final DataTableSpec spec) throws NotConfigurableException;

    /** Saves settings to config object. Argument can be safely type casted to
     * expected config class.
     * @param config To save to.
     * @throws InvalidSettingsException If user entered invalid values.
     */
    public abstract void saveSettings(final AbstractFileTypeWriteConfig config)
        throws InvalidSettingsException;

}
