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
 *   Feb 2, 2010 (wiswedel): created
 */
package org.knime.exttool.executor;

import java.util.Observable;

import org.knime.core.node.ExecutionMonitor;

/**
 * Executor for an external application. It does the process handling in
 * the {@link #execute(ExecutionMonitor)} method, i.e. invoking the commandline.
 * If the external tool node is run in chunks (or even row-by-row), each of
 * the chunks constitutes an individual execution.
 *
 * <p>
 * None of the methods is meant to be called by client code, except for
 * sub-classes, which implement the <code>execute</code> logic. All of the
 * <code>get</code> methods return meaningful information only during the
 * execution.
 *
 * <p><b>Warning:</b> API needs review, subclassing outside this package
 * is currently not encouraged.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public abstract class AbstractExttoolExecutor extends Observable {

    private ExecutionChunkCallable m_executionChunkCallable;

    /**
     * Get the final commandline. Any of the placeholders such as %inFile%
     * will be replaced by the final file location.
     * @return the command line arguments, ready to be passed to, e.g.
     * {@link Runtime#exec(String[])}.
     * @see ExecutionChunkCallable#getCommandlineArgs()
     */
    protected final String[] getCommandlineArgs() {
        return getExecutionChunkCallable().getCommandlineArgs();
    }

    /**
     * Get the input handles that are associated with this execution. Each
     * element in the returned array can safely be type-casted to the class
     * that is returned by the corresponding factory's
     * {@link AbstractExttoolExecutorFactory#createInputDataHandle(
     *  org.knime.exttool.node.ExttoolSettings, java.io.File)} method.
     * @return input handles for this execution.
     * @see ExecutionChunkCallable#getInputHandles()
     */
    protected final InputDataHandle[] getInputHandles() {
        return getExecutionChunkCallable().getInputHandles();
    }

    /**
     * Get the output handles that are associated with this execution. Each
     * element in the returned array can safely be type-casted to the class
     * that is returned by the corresponding factory's
     * {@link AbstractExttoolExecutorFactory#createOutputDataHandle(
     *  org.knime.exttool.node.ExttoolSettings, java.io.File)} method.
     * @return output handles for this execution.
     * @see ExecutionChunkCallable#getOutputHandles()
     */
    protected final OutputDataHandle[] getOutputHandles() {
        return getExecutionChunkCallable().getOutputHandles();
    }

    /** Get callable, throw exception when not set (asserts proper execution).
     * @return the callable for this execution.
     */
    private ExecutionChunkCallable getExecutionChunkCallable() {
        if (m_executionChunkCallable == null) {
            throw new IllegalStateException("No execution chunk associated,"
                    + " it must be set before the main execution");
        }
        return m_executionChunkCallable;
    }

    /** Set the callable. This method is called shortly before the
     * {@link #execute(ExecutionMonitor)} is called.
     * @param executionChunkCallable the callable to set
     */
    final void setExecutionChunkCallable(
            final ExecutionChunkCallable executionChunkCallable) {
        m_executionChunkCallable = executionChunkCallable;
    }

    /** Launches the process and returns the process' exit code. This method
     * needs to create the output files. It is run a separate thread, subclasses
     * should evaluate the monitors cancellation flag and/or the calling threads
     * interrupt status.
     * @param monitor The monitor for possible progress report and cancellation.
     * @return The exit code of the external process. A non-zero exit code
     *         represents and error.
     * @throws Exception any exception that is appropriate to indicate an error.
     */
    protected abstract int execute(final ExecutionMonitor monitor)
        throws Exception;

}
