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
 *   Jan 18, 2010 (wiswedel): created
 */
package org.knime.exttool.node;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * Dialog of the external tool node. It combines a main panel and additional
 * panels for input and output selection.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class ExttoolNodeDialogPane extends NodeDialogPane {

    private final ExttoolCustomizer m_dialogCustomizer;
    private ExternalToolPanel m_externalToolPanel;
    private InputFilePanel m_inputFilePanel;
    private OutputFilePanel m_outputFilePanel;
    private ExecutorPanel m_executorPanel;

    /** Create new dialog based on a given customizer. The caller must
     * invoke {@link #initLayout()} right after construction.
     * @param dialogCustomizer The customizer as constructed
     *        in the node factory.
     */
    public ExttoolNodeDialogPane(
            final ExttoolCustomizer dialogCustomizer) {
        if (dialogCustomizer == null) {
            throw new NullPointerException("Argument must not be null");
        }
        m_dialogCustomizer = dialogCustomizer;
    }

    /** Does the layout, to be called right after dialog construction. */
    protected void initLayout() {
        m_externalToolPanel = createExternalToolPanel();
        m_externalToolPanel.initLayout();
        m_inputFilePanel = createInputFilePanel();
        m_inputFilePanel.initLayout();
        m_outputFilePanel = createOutputFilePanel();
        m_outputFilePanel.initLayout();
        m_executorPanel = createExecutorPanel();
        m_executorPanel.initLayout();
        addTab("External Tool", m_externalToolPanel);
        if (m_dialogCustomizer.isShowTabInputFile()) {
            addTab("Input File", m_inputFilePanel);
        }
        if (m_dialogCustomizer.isShowTabOutputFile()) {
            addTab("Output File", m_outputFilePanel);
        }
        if (m_dialogCustomizer.isShowTabExecutorPanel()) {
            addTab("Executor", m_executorPanel);
        }
    }

    /** Create the main panel.
     * @return A new {@link ExternalToolPanel} using the customizer passed
     *         in the constructor.
     */
    protected ExternalToolPanel createExternalToolPanel() {
        return new ExternalToolPanel(m_dialogCustomizer);
    }

    /** Create the input panel.
     * @return A new {@link InputFilePanel} using the customizer passed
     *         in the constructor.
     */
    protected InputFilePanel createInputFilePanel() {
        return new InputFilePanel(m_dialogCustomizer);
    }

    /** Create the output panel.
     * @return A new {@link OutputFilePanel} using the customizer passed
     *         in the constructor.
     */
    protected OutputFilePanel createOutputFilePanel() {
        return new OutputFilePanel(m_dialogCustomizer);
    }

    /** Create the output panel.
     * @return A new {@link OutputFilePanel} using the customizer passed
     *         in the constructor.
     */
    protected ExecutorPanel createExecutorPanel() {
        return new ExecutorPanel(m_dialogCustomizer);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] rawInSpecs) throws NotConfigurableException {
        DataTableSpec[] inSpecs =
            m_dialogCustomizer.preprocessInput(rawInSpecs);
        ExttoolSettings exttoolSettings =
            m_dialogCustomizer.createExttoolSettings();
        exttoolSettings.loadSettingsFrom(settings, inSpecs);
        m_inputFilePanel.loadSettingsFrom(exttoolSettings, inSpecs);
        m_outputFilePanel.loadSettingsFrom(exttoolSettings);
        m_executorPanel.loadSettingsFrom(exttoolSettings);
        m_externalToolPanel.loadSettingsFrom(exttoolSettings, inSpecs);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        ExttoolSettings exttoolSettings =
            m_dialogCustomizer.createExttoolSettings();
        m_inputFilePanel.saveSettingsTo(exttoolSettings);
        m_outputFilePanel.saveSettingsTo(exttoolSettings);
        m_executorPanel.saveSettingsTo(exttoolSettings);
        m_externalToolPanel.saveSettingsTo(exttoolSettings);
        exttoolSettings.saveSettingsToInDialog(settings);
    }

}
