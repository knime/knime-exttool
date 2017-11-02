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
package org.knime.exttool.node;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.exttool.executor.AbstractExttoolExecutorConfig;
import org.knime.exttool.executor.AbstractExttoolExecutorConfigPanel;
import org.knime.exttool.executor.AbstractExttoolExecutorFactory;

/**
 * Panel that allows the user to select and customize an executor.
 * @see AbstractExttoolExecutorFactory
 * @see AbstractExttoolExecutorConfigPanel
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("serial")
public class ExecutorPanel extends JPanel {

    private final JComboBox m_executorCombo;
    private final Map<AbstractExttoolExecutorFactory,
        AbstractExttoolExecutorConfigPanel> m_factoryToControlMap;
    private final JPanel m_centerPanel;
    private Dimension m_centerPrefSize;

    /** Create new panel. Argument is currently ignored.
     * @param customizer The customizer defining node.
     */
    public ExecutorPanel(final ExttoolCustomizer customizer) {
        super(new BorderLayout());
        if (customizer == null) {
            throw new NullPointerException("Argument must not be null");
        }
        m_centerPanel = new JPanel(new BorderLayout());
        m_factoryToControlMap = new HashMap<AbstractExttoolExecutorFactory,
        AbstractExttoolExecutorConfigPanel>();
        Collection<AbstractExttoolExecutorFactory> factories =
            AbstractExttoolExecutorFactory.getFactories();
        Object[] asArray = new Object[factories.size()];
        int counter = 0;
        for (AbstractExttoolExecutorFactory f : factories) {
            asArray[counter++] = f;
            m_factoryToControlMap.put(f, f.createConfig().createConfigPanel());
        }
        m_executorCombo = new JComboBox(asArray);
        m_executorCombo.setRenderer(ExttoolFactoryRenderer.INSTANCE);
        m_executorCombo.addItemListener(new ItemListener() {
            /** {@inheritDoc} */
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    onExecutorChanged();
                }
            }
        });
        onExecutorChanged();
    }

    /** Called after construction to init layout. */
    protected void initLayout() {
        JPanel northPanel = new JPanel(new FlowLayout());
        if (m_factoryToControlMap.size() == 1) {
            // only local executor -- do not show combo but only label
            AbstractExttoolExecutorFactory val =
                m_factoryToControlMap.keySet().iterator().next();
            northPanel.add(new JLabel("Only " + val.getName() + " available"));
        } else {
            northPanel.add(new JLabel("Executor: "));
            northPanel.add(m_executorCombo);
        }
        add(northPanel, BorderLayout.NORTH);
        add(m_centerPanel, BorderLayout.CENTER);
    }

    /** Called when a new executor is selected in the combo box. */
    private void onExecutorChanged() {
        m_centerPanel.removeAll();
        AbstractExttoolExecutorFactory selected =
            (AbstractExttoolExecutorFactory)m_executorCombo.getSelectedItem();
        if (selected != null) {
            AbstractExttoolExecutorConfigPanel configPanel =
                m_factoryToControlMap.get(selected);
            if (m_centerPrefSize != null) {
                configPanel.setPreferredSize(m_centerPrefSize);
            }
            m_centerPanel.add(configPanel);
        }
        m_centerPanel.revalidate();
        m_centerPanel.repaint();
    }

    /** Loads settings while dialog is opening.
     * @param settings To load from.
     * @throws NotConfigurableException If no valid config possible.
     */
    public void loadSettingsFrom(final ExttoolSettings settings)
        throws NotConfigurableException {
        if (m_centerPrefSize == null) {
            // activate each panel once, keep max dimension
            int maxWidth = -1;
            int maxHeight = -1;
            for (Map.Entry<AbstractExttoolExecutorFactory,
                    AbstractExttoolExecutorConfigPanel> entry
                    : m_factoryToControlMap.entrySet()) {
                // changes center panel
                m_executorCombo.setSelectedItem(entry.getKey());
                Dimension prefSize = entry.getValue().getPreferredSize();
                maxWidth = Math.max(prefSize.width, maxWidth);
                maxHeight = Math.max(prefSize.height, maxHeight);
            }
            if (maxWidth > 0) {
                m_centerPrefSize = new Dimension(maxWidth, maxHeight);
            }
        }
        AbstractExttoolExecutorFactory executor = settings.getExecutorFactory();
        AbstractExttoolExecutorConfig config = settings.getExecutorConfig();
        m_executorCombo.setSelectedItem(executor);
        AbstractExttoolExecutorConfigPanel configPanel =
                m_factoryToControlMap.get(executor);
        if (configPanel != null) {
            configPanel.loadSettings(config);
        }
    }

    /** Saves current configuration.
     * @param settings To save to.
     * @throws InvalidSettingsException if that fails.
     */
    public void saveSettingsTo(final ExttoolSettings settings)
        throws InvalidSettingsException {
        AbstractExttoolExecutorFactory factory =
            (AbstractExttoolExecutorFactory)m_executorCombo.getSelectedItem();
        AbstractExttoolExecutorConfigPanel panel =
            m_factoryToControlMap.get(factory);
        AbstractExttoolExecutorConfig config = factory.createConfig();
        panel.saveSettings(config);
        settings.setExecutor(factory, config);
    }

    /** Combo box renderer for executor factories. */
    private static final class ExttoolFactoryRenderer
        extends DefaultListCellRenderer {

        private static final ExttoolFactoryRenderer INSTANCE =
            new ExttoolFactoryRenderer();

        /** {@inheritDoc} */
        @Override
        public Component getListCellRendererComponent(final JList list,
                final Object value, final int index, final boolean isSelected,
                final boolean cellHasFocus) {
            Object newObject;
            if (value instanceof AbstractExttoolExecutorFactory) {
                setToolTipText("Class \"" + value.getClass().getName() + "\"");
                newObject = ((AbstractExttoolExecutorFactory)value).getName();
            } else {
                setToolTipText(null);
                newObject = value;
            }
            return super.getListCellRendererComponent(
                    list, newObject, index, isSelected, cellHasFocus);
        }

    }

}
