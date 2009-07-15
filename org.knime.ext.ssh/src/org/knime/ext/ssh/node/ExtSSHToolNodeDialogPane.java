/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 *
 * History
 *   Jul 13, 2009 (ohl): created
 */
package org.knime.ext.ssh.node;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 *
 * @author Peter Ohl, KNIME.com, Zurich, Switzerland
 */
public class ExtSSHToolNodeDialogPane extends NodeDialogPane {

    private final ExtSSHToolSettingsPanel m_panel =
            new ExtSSHToolSettingsPanel();

    /**
     * The constructor of this class with no arguments and a very long java doc
     * comment.
     */
    public ExtSSHToolNodeDialogPane() {
        addTab("Ext SSH Tool Settings", m_panel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        ExtSSHToolSettings s = new ExtSSHToolSettings();
        m_panel.saveSettings(s);
        String msg = s.getStatusMsg();
        if (msg != null) {
            throw new InvalidSettingsException(msg);
        }
        s.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        ExtSSHToolSettings s = new ExtSSHToolSettings();
        try {
            s = new ExtSSHToolSettings(settings);
        } catch (InvalidSettingsException ise) {
            // keep the empty defaults
        }
        m_panel.loadSettings(s);
    }



}
