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
 *   Jun 23, 2010 (wiswedel): created
 */
package org.knime.exttool.executor;

import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;

/**
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
@SuppressWarnings("serial")
public class DefaultExttoolExecutorConfigPanel extends
        AbstractExttoolExecutorConfigPanel {

    private final JSpinner m_threadCountSpinner;
    private final JCheckBox m_autoThreaderChecker;

    /**
     *
     */
    public DefaultExttoolExecutorConfigPanel() {
        super(new GridLayout(0, 1));
        final int def = DefaultExttoolExecutorConfig.getAutoThreadCount();
        m_autoThreaderChecker = new JCheckBox("Set automatically");
        m_autoThreaderChecker.addChangeListener(new ChangeListener() {
            /** {@inheritDoc} */
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_threadCountSpinner.setEnabled(
                        !m_autoThreaderChecker.isSelected());
            }
        });
        m_autoThreaderChecker.setToolTipText("Set thread count slighly larger "
                + "than the system's CPU count (" + def + ")");
        m_threadCountSpinner = new JSpinner(new SpinnerNumberModel(
                def, 1, Integer.MAX_VALUE, 1));
        setBorder(BorderFactory.createTitledBorder(
                "Parallel process count (only when chunking enabled)"));
        add(getInFlowLayout(m_autoThreaderChecker));
        add(getInFlowLayout(m_threadCountSpinner));
        m_autoThreaderChecker.doClick();
    }

    private static final JPanel getInFlowLayout(final JComponent... comps) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JComponent c : comps) {
            panel.add(c);
        }
        return panel;
    }

    /** {@inheritDoc} */
    @Override
    public void loadSettings(final AbstractExttoolExecutorConfig config)
            throws NotConfigurableException {
        DefaultExttoolExecutorConfig c = (DefaultExttoolExecutorConfig)config;
        m_threadCountSpinner.setValue(c.getMaxThreads());
        m_autoThreaderChecker.setSelected(c.isAutoThreadCount());
    }

    /** {@inheritDoc} */
    @Override
    public void saveSettings(final AbstractExttoolExecutorConfig config)
            throws InvalidSettingsException {
        DefaultExttoolExecutorConfig c = (DefaultExttoolExecutorConfig)config;
        c.setAutoThreadCount(m_autoThreaderChecker.isSelected());
        c.setMaxThreads((Integer)m_threadCountSpinner.getValue());
    }

}
