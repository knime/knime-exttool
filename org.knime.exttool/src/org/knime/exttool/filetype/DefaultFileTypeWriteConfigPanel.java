/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   Apr 13, 2010 (wiswedel): created
 */
package org.knime.exttool.filetype;

import java.awt.FlowLayout;

import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/** Default write (input) config panel showing a column selection combo box to
 * select a single column.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
@SuppressWarnings("serial")
public class DefaultFileTypeWriteConfigPanel extends
        AbstractFileTypeWriteConfigPanel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            DefaultFileTypeWriteConfigPanel.class);

    private final ColumnSelectionComboxBox m_columnBox;

    /** Constructor inherited from super class.
     * @param columnFilter */
    public DefaultFileTypeWriteConfigPanel(final ColumnFilter columnFilter) {
        super(new FlowLayout());
        m_columnBox = new ColumnSelectionComboxBox((Border)null, columnFilter);
        add(m_columnBox);
    }

    /** (Config argument must be a (subclass of)
     * {@link DefaultFileTypeReadConfig}).
     * {@inheritDoc} */
    @Override
    public void loadSettings(final AbstractFileTypeWriteConfig config,
            final DataTableSpec spec) {
        DefaultFileTypeWriteConfig c = (DefaultFileTypeWriteConfig)config;
        String column = c.getColumn();
        try {
            m_columnBox.update(spec, column);
        } catch (NotConfigurableException e) {
            LOGGER.warn("Caught " + e.getClass().getSimpleName() + " although"
                    + " config argument is supposed to have a valid "
                    + "configuration", e);
        }
    }

    /** (Config argument must be a (subclass of)
     * {@link DefaultFileTypeReadConfig}).
     * {@inheritDoc} */
    @Override
    public void saveSettings(final AbstractFileTypeWriteConfig config)
            throws InvalidSettingsException {
        DefaultFileTypeWriteConfig c = (DefaultFileTypeWriteConfig)config;
        String column = m_columnBox.getSelectedColumn();
        c.setColumn(column);
    }


}
