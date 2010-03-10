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
 *   Mar 6, 2010 (wiswedel): created
 */
package org.knime.exttool.node.base;

import java.util.Arrays;
import java.util.concurrent.Callable;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.exttool.executor.AbstractExttoolExecutor;
import org.knime.exttool.executor.InputDataHandle;
import org.knime.exttool.executor.OutputDataHandle;
import org.knime.exttool.filetype.AbstractFileTypeRead;

/**
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class ExecutionChunkCallable implements Callable<BufferedDataTable[]> {

    private InputDataHandle[] m_inputHandles;
    private OutputDataHandle[] m_outputHandles;
    private AbstractFileTypeRead[] m_outputFileTypes;
    private String[] m_commandlineArgs;
    private ExecutionContext m_context;

    private final AbstractExttoolExecutor m_executor;

    /**
     *
     */
    protected ExecutionChunkCallable(final AbstractExttoolExecutor executor) {
        if (executor == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_executor = executor;
    }

    /** {@inheritDoc} */
    public BufferedDataTable[] call() throws Exception {
        m_executor.setExecutionChunkCallable(this);
        if (m_context == null) {
            throw new IllegalStateException("No execution context set on "
                    + getClass().getSimpleName());
        }
        int exitCode = m_executor.execute(m_context);
        if (exitCode != 0) {
            throw new Exception("Failed with exit code " + exitCode);
        }
        return readResults();
    }

    protected BufferedDataTable[] readResults() throws Exception {
        ExecutionContext context = getExecutionContext();
        final int outCount = m_outputFileTypes.length;
        BufferedDataTable[] result = new BufferedDataTable[outCount];
        final double prog = 1.0 / outCount;
        for (int i = 0; i < outCount; i++) {
            ExecutionContext sub = context.createSubExecutionContext(prog);
            AbstractFileTypeRead read = m_outputFileTypes[i];
            OutputDataHandle outHandle = m_outputHandles[i];
            result[i] = read.readTable(outHandle, sub);
        }
        return result;
    }

    /**
     * @return the commandlineArgs
     */
    public final String[] getCommandlineArgs() {
        return m_commandlineArgs;
    }

    /**
     * @return the inputHandles
     */
    public final InputDataHandle[] getInputHandles() {
        return m_inputHandles;
    }

    /**
     * @return the outputHandles
     */
    public final OutputDataHandle[] getOutputHandles() {
        return m_outputHandles;
    }

    /**
     * @return the outputFileTypes
     */
    public final AbstractFileTypeRead[] getOutputFileTypes() {
        return m_outputFileTypes;
    }

    /**
     * @return the context
     */
    public ExecutionContext getExecutionContext() {
        return m_context;
    }

    /**
     * @param commandlineArgs the commandlineArgs to set
     */
    final void setCommandlineArgs(final String[] commandlineArgs) {
        m_commandlineArgs = commandlineArgs;
    }

    /**
     * @param inputHandles the inputHandles to set
     */
    final void setInputHandles(final InputDataHandle[] inputHandles) {
        if (Arrays.asList(inputHandles).contains(null)) {
            throw new NullPointerException("Input must not contain null.");
        }
        m_inputHandles = inputHandles;
    }

    /**
     * @param outputHandles the outputHandles to set
     */
    final void setOutputHandles(final OutputDataHandle[] outputHandles) {
        if (Arrays.asList(outputHandles).contains(null)) {
            throw new NullPointerException("Output must not contain null.");
        }
        if (m_outputFileTypes != null
                && m_outputFileTypes.length != outputHandles.length) {
            throw new IllegalArgumentException("Output file type array "
                    + "length is different from output data handle array: "
                    + m_outputFileTypes.length + " vs. "
                    + outputHandles.length);
        }
        m_outputHandles = outputHandles;
    }

    /**
     * @param fileTypes the outputFileTypes to set
     */
    final void setOutputFileTypes(final AbstractFileTypeRead[] fileTypes) {
        if (Arrays.asList(fileTypes).contains(null)) {
            throw new NullPointerException("Output must not contain null.");
        }
        if (m_outputHandles != null
                && m_outputHandles.length != fileTypes.length) {
            throw new IllegalArgumentException("Output file type array "
                    + "length is different from output data handle array: "
                    + fileTypes.length + " vs. " + m_outputHandles.length);
        }
        m_outputFileTypes = fileTypes;
    }

    /**
     * @param context the context to set
     */
    final void setExecutionContext(final ExecutionContext context) {
        m_context = context;
    }

}
