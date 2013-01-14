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
 *   Jun 23, 2010 (wiswedel): created
 */
package org.knime.exttool.executor;

import java.util.concurrent.ExecutorService;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.exttool.node.ExttoolNodeModel;

/**
 * Settings object that represents the configuration of an executor. For a
 * default executor (local execution), this could be a total number of
 * parallel threads (chunking), for an SSH executor the connection information
 * for a remote host and for a queuing system executor some queue information.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public abstract class AbstractExttoolExecutorConfig {

    /** Saves the current state to the argument.
     * @param settings To save to. */
    public abstract void saveSettings(final NodeSettingsWO settings);

    /** Load a state from a settings object. This method is called from the
     * load method in the {@link ExttoolNodeModel}.
     * @param settings To load configuration from.
     * @throws InvalidSettingsException If settings are incomplete or invalid.
     */
    public abstract void loadSettingsInModel(final NodeSettingsRO settings)
        throws InvalidSettingsException;

    /** Load the settings in the dialog, falling back to default in case of
     * errors.
     * @param settings To load configuration from.
     * @throws NotConfigurableException If no valid configuration is possible.
     *         (this shouldn't happen as the executor configuration is
     *         independent from the input data; this exception is declared
     *         as all dialog loading methods can fail with it)
     */
    public abstract void loadSettingsInDialog(final NodeSettingsRO settings)
        throws NotConfigurableException;

    /** Create a new config panel for this configuration object. The returned
     * instance can safely cast the config object to this concrete class in
     * its save and load methods.
     * @return A new controller.
     */
    public abstract AbstractExttoolExecutorConfigPanel createConfigPanel();

    /** Get an executor service that is used to run the task(s). This can
     * be an unbounded thread pool (which runs tasks as they arrive, useful,
     * e.g. when jobs are run remotely and the threads just wait for the jobs to
     * finish) or a bounded thread pool which executes at most a given
     * number of tasks.
     *
     * <p>This method should not actually be used from custom executor
     * implementations. It is used from the {@link Execution} class to submit
     * sub-jobs.
     *
     * @return an executor service to run the job(s), one job if entire table
     * is to be processed or multiple jobs when chunking is enabled.
     */
    abstract ExecutorService createExecutorService();

}
