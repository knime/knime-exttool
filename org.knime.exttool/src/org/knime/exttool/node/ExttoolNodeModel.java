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
 *   Jan 19, 2010 (wiswedel): created
 */
package org.knime.exttool.node;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.exttool.executor.Execution;

/** Default node model for the external tool model. It mostly just delegates
 * to the executor and settings that are created by the
 * {@link ExttoolCustomizer}.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class ExttoolNodeModel extends NodeModel {

    private final ExttoolCustomizer m_customizer;
    private ExttoolSettings m_settings;

    /** Create new model, using in/out count as in the customizer.
     * @param customizer The configuration object.
     */
    protected ExttoolNodeModel(final ExttoolCustomizer customizer) {
        super(customizer.getNrInputs(), customizer.getNrOutputs());
        m_customizer = customizer;
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_settings == null) {
            throw new InvalidSettingsException("No settings available");
        }
        DataTableSpec[] newInSpecs = m_customizer.preprocessInput(inSpecs);
        // have a new execution object create the output spec.
        ExttoolNodeEnvironment env = new ExttoolNodeEnvironment(this);
        Execution execution = m_customizer.createExecution(m_settings, env);
        return execution.configure(newInSpecs);
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        ExttoolNodeEnvironment env = new ExttoolNodeEnvironment(this);
        Execution execution = m_customizer.createExecution(m_settings, env);
        BufferedDataTable[] newInData = m_customizer.preprocessInput(
                inData, exec.createSubExecutionContext(0.0));
        BufferedDataTable[] result;
        try {
            result = execution.execute(newInData, exec);
        } finally {
            execution.cleanUp();
        }
        String warningMessage = execution.clearWarningMessage();
        if (warningMessage != null) {
            setWarningMessage(warningMessage);
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        // nothing to do (might to clear logs once we have a view)
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_customizer.createExttoolSettings().loadSettingsFrom(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        ExttoolSettings exttoolSettings =
            m_customizer.createExttoolSettings();
        exttoolSettings.loadSettingsFrom(settings);
        m_settings = exttoolSettings;
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_settings != null) {
            m_settings.saveSettingsTo(settings);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to save (might need to save execution status messages?)
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to load (might need to save execution status messages?)
    }

    /** Delegate to super to read flow variable.
     * @param name Name of variable
     * @return Value of variable
     * @throws NoSuchElementException as super throws exception (invalid var)
     */
    String readFlowVariableString(final String name) {
        return peekFlowVariableString(name);
    }

    /** Delegate to super to read flow variable.
     * @param name Name of variable
     * @return Value of variable
     * @throws NoSuchElementException as super throws exception (invalid var)
     */
    double readFlowVariableDouble(final String name) {
        return peekFlowVariableDouble(name);
    }

    /** Delegate to super to read flow variable.
     * @param name Name of variable
     * @return Value of variable
     * @throws NoSuchElementException as super throws exception (invalid var)
     */
    int readFlowVariableInt(final String name) {
        return peekFlowVariableInt(name);
    }

}
