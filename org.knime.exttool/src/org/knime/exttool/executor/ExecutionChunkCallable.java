/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   Mar 6, 2010 (wiswedel): created
 */
package org.knime.exttool.executor;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.BlobSupportDataRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.util.ThreadUtils.CallableWithContext;
import org.knime.exttool.filetype.AbstractFileTypeRead;

/**
 * A {@link Callable} that runs the external process. Upon {@link #call()} it
 * will have its entire environment set up (e.g. input files to the external
 * tool are written and command line is created).
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class ExecutionChunkCallable extends CallableWithContext<BufferedDataTable[]> {

    /** input handles, length equals number of input ports. */
    private InputDataHandle[] m_inputHandles;

    /** output handles, length equals number of output ports. */
    private OutputDataHandle[] m_outputHandles;

    /** output file type readers for parsing the result,
     * length equals number of output ports. */
    private AbstractFileTypeRead[] m_outputFileTypes;

    /** pre-compiled command line args (placeholders such %inFile% already
     * replaced by actual path. */
    private String[] m_commandlineArgs;

    private ExecutionContext m_context;

    /** An atomic integer that is used to create unique row IDs in the result
     * tables. See {@link #setRowIdUnifier(AtomicInteger)} for details. */
    private AtomicInteger m_rowIdUnifier;

    /** Executor for this job/chunk. */
    private final AbstractExttoolExecutor m_executor;

    /** Config to the executor. */
    private AbstractExttoolExecutorConfig m_executorConfig;

    /** Create new chunk callable based for the associated executor.
     * @param executor The (non-null) executor.
     */
    protected ExecutionChunkCallable(final AbstractExttoolExecutor executor) {
        if (executor == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_executor = executor;
    }

    /** Prepares the executor and finally calls its
     * {@link AbstractExttoolExecutor#execute(ExecutionMonitor)} method.
     * @return The output data
     * @exception Exception Any exception.
     * @see Callable#call()
     * @since 2.8
     */
    @Override
    protected final BufferedDataTable[] callWithContext() throws Exception {
        m_executor.setExecutionChunkCallable(this);
        ExecutionMonitor mainMon;
        ExecutionContext readContext;
        ExecutionContext postContext;
        if (m_rowIdUnifier != null) {
            mainMon = m_context.createSubExecutionContext(0.7);
            readContext = m_context.createSubExecutionContext(0.2);
            postContext = m_context.createSubExecutionContext(0.1);
        } else {
            mainMon = m_context.createSubExecutionContext(0.75);
            readContext = m_context.createSubExecutionContext(0.25);
            postContext = m_context.createSubExecutionContext(0);
        }
        if (m_context == null) {
            throw new IllegalStateException("No execution context set on "
                    + getClass().getSimpleName());
        }
        int exitCode = m_executor.execute(mainMon);
        if (exitCode != 0) {
            throw new Exception("Failed with exit code " + exitCode);
        }
        mainMon.setProgress(1.0);
        m_context.setMessage("Reading results");
        BufferedDataTable[] tables = readResults(readContext);
        readContext.setProgress(1.0);
        if (m_rowIdUnifier != null && tables.length > 0) {
            m_context.setMessage("Creating unique row identifiers "
                        + "for table concatenation");
            tables[0] = uniquifyRowIdsPort0(tables[0], postContext);
            postContext.setProgress(1.0);
        }
        return tables;
    }

    /** Called after execution to read the final results.
     * @param context For cancellation/progress report and table creation.
     * @return The output tables, read from the corresponding
     *         {@link #getOutputHandles()}.
     * @throws Exception In case of read errors, e.g.
     */
    protected BufferedDataTable[] readResults(
            final ExecutionContext context) throws Exception {
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

    /** Post-processing step to ensure unique row IDs in the output tables of
     * all chunks. See {@link #setRowIdUnifier(AtomicInteger)} for details.
     * @param table The table to be uniquified.
     * @param exec for progress/cancel/table creation.
     * @return The output table with new row IDs.
     * @throws CanceledExecutionException If canceled.
     */
    protected BufferedDataTable uniquifyRowIdsPort0(
            final BufferedDataTable table, final ExecutionContext exec)
        throws CanceledExecutionException {
        if (m_rowIdUnifier == null) {
            return table;
        }
        BufferedDataContainer cont = exec.createDataContainer(table.getSpec());
        int i = 0;
        final double rowCount = table.getRowCount();
        for (DataRow r : table) {
            RowKey key = new RowKey("R" + m_rowIdUnifier.getAndIncrement());
            cont.addRowToTable(new BlobSupportDataRow(key, r));
            exec.checkCanceled();
            exec.setProgress(i++ / rowCount, "Row " + i + "/" + rowCount);
        }
        cont.close();
        return cont.getTable();
    }

    /** @return the (final) commandlineArgs, no placeholders left inside.
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
     * @return The executor config.
     */
    public final AbstractExttoolExecutorConfig getExecutorConfig() {
        return m_executorConfig;
    }

    /**
     * @return the context
     */
    public ExecutionContext getExecutionContext() {
        return m_context;
    }

    /** Calls {@link DataHandle#cleanUp()} on all in/output handles. */
    public void cleanUp() {
        if (m_inputHandles != null) {
            for (InputDataHandle in : m_inputHandles) {
                in.cleanUp();
            }
        }
        if (m_outputHandles != null) {
            for (OutputDataHandle out : m_outputHandles) {
                out.cleanUp();
            }
        }
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
     * @param executorConfig the executorConfig to set
     */
    void setExecutorConfig(final AbstractExttoolExecutorConfig executorConfig) {
        m_executorConfig = executorConfig;
    }

    /**
     * @param context the context to set
     */
    final void setExecutionContext(final ExecutionContext context) {
        m_context = context;
    }

    /** Set by the framework when the execution is split into chunks. Each of
     * the chunks potentially creates the same set of (default) row IDs, which
     * need to be concatenated in a post-processing step. This atomic integer
     * is used to create unique row IDs in the
     * {@link #uniquifyRowIdsPort0(BufferedDataTable, ExecutionContext)} method.
     *
     * @param rowIdUnifier the rowIdUnifier to set
     */
    final void setRowIdUnifier(final AtomicInteger rowIdUnifier) {
        m_rowIdUnifier = rowIdUnifier;
    }

}
