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
 *   Feb 19, 2010 (wiswedel): created
 */
package org.knime.exttool.executor;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.BlobSupportDataRow;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.core.util.ThreadPool;
import org.knime.exttool.filetype.AbstractFileTypeRead;
import org.knime.exttool.filetype.AbstractFileTypeWrite;
import org.knime.exttool.node.ExttoolCustomizer;
import org.knime.exttool.node.ExttoolSettings;
import org.knime.exttool.node.ExttoolSettings.PathAndTypeConfiguration;

/**
 * Contains the logic of setting up and running all the executions. There is
 * one single object of this class responsible for the entire execution of a
 * node, that is, it also controls all the thread-handling for individual
 * chunks. None of the methods, except for the
 * {@link #execute(BufferedDataTable[], ExecutionContext) execute method} is
 * meant to be called outside this class or a subclass of it.

 * <p><b>Warning:</b> API needs review, subclassing outside this package
 * is currently not encouraged.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class Execution {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(Execution.class);

    private final ExttoolCustomizer m_customizer;
    private final ExttoolSettings m_settings;

    /** Working directory, created based on the first valid in/output file.*/
    private File m_workingDirectory;
    /** Directory in the working dir containing input data to the ext-tool. */
    private File m_inputDirectory;
    /** Directory in the working dir containing output data created by
     * the external tool. */
    private File m_outputDirectory;

    /** Warning messages that are collected throughout the execution. */
    private final StringBuilder m_warningMessageBuilder = new StringBuilder();

    /** Creates new execution for a given customizer and the (validated!)
     * settings.
     * @param customizer The customizer for this node ("static" information)
     * @param settings The node settings.
     */
    public Execution(final ExttoolCustomizer customizer,
            final ExttoolSettings settings) {
        if (customizer == null || settings == null) {
            throw new NullPointerException("Arguments must not be null.");
        }
        m_customizer = customizer;
        m_settings = settings;
    }

    /** @return the customizer passed in the constructor. */
    protected ExttoolCustomizer getCustomizer() {
        return m_customizer;
    }

    /** @return the settings passed in the constructor. */
    protected ExttoolSettings getSettings() {
        return m_settings;
    }

    /** Called from the {@link #execute(BufferedDataTable[], ExecutionContext)
     * execute method} to write the input and create the execution callables.
     * This method is not be called outside this class (but it can be
     * overwritten to take additional steps that are required in preparation of
     * the execution).
     * @param inputTables The input table of this node.
     * @param exec Progress monitor for cancellation/progress
     * @return A list of callables that are run by the <code>execute</code>
     * @throws IOException If there problems writing the input data.
     * @throws InvalidSettingsException If the settings aren't OK
     * @throws CanceledExecutionException If canceled.
     */
    protected List<ExecutionChunkCallable> prepareExecution(
            final BufferedDataTable[] inputTables, final ExecutionMonitor exec)
            throws IOException, InvalidSettingsException,
            CanceledExecutionException {
        final ExttoolSettings settings = getSettings();
        final int nrInPorts = getCustomizer().getNrInputs();
        final int nrOutPorts = getCustomizer().getNrOutputs();
        AbstractExttoolExecutorFactory executorFactory =
            settings.getExecutorFactory();
        List<ExecutionChunkCallable> result =
            new ArrayList<ExecutionChunkCallable>();
        int chunkSize = settings.getChunkSize();
        int rowsPerChunkFirstPort = -1;
        if (nrInPorts > 0) {
            rowsPerChunkFirstPort = inputTables[0].getRowCount();
        }
        int chunkCount = 1;
        if (chunkSize > 0 && nrInPorts > 0) {
            // only first input port will be chunked
            int rowCount = inputTables[0].getRowCount();
            chunkCount = (int)Math.ceil(rowCount / (double)chunkSize);
            rowsPerChunkFirstPort =
                (int)Math.ceil(rowCount / (double)chunkCount);
        }

        InputDataHandle[][] inputHandles =
            new InputDataHandle[nrInPorts][chunkCount];
        OutputDataHandle[][] outputHandles =
            new OutputDataHandle[nrOutPorts][chunkCount];
        AbstractFileTypeRead[] outputFileTypes =
            new AbstractFileTypeRead[nrOutPorts];

        // determine input file names and write input data
        for (int port = 0; port < nrInPorts; port++) {
            exec.setMessage("Writing port " + port);
            ExecutionMonitor subProg = exec.createSubProgress(1.0 / nrInPorts);
            BufferedDataTable table = inputTables[port];
            AbstractFileTypeWrite fileType = settings.createInputFileType(port);
            RowIterator it = table.iterator();
            if (port == 0 && chunkCount > 1) {
                for (int chunkIdx = 0; chunkIdx < chunkCount; chunkIdx++) {
                    int startRowIncl = chunkIdx * rowsPerChunkFirstPort;
                    int endRowExcl = startRowIncl + rowsPerChunkFirstPort;
                    File suggestedInputFile = getInputFilePath(
                            port, chunkIdx, fileType);
                    RowIterator filterIterator =
                        new ViewRowIterator(it, endRowExcl - startRowIncl);
                    InputDataHandle inputHandle =
                        executorFactory.createInputDataHandle(
                            settings, suggestedInputFile);
                    OutputStream out = inputHandle.openInputFileOutStream();
                    subProg.setMessage("Chunk " + chunkIdx);
                    ExecutionMonitor subProg2 =
                        subProg.createSubProgress(1.0 / chunkCount);
                    fileType.writeTable(table.getDataTableSpec(),
                            filterIterator, endRowExcl - startRowIncl,
                            out, subProg2);
                    out.close();
                    inputHandles[port][chunkIdx] = inputHandle;
                }
            } else {
                File suggestedInputFile = getInputFilePath(port, -1, fileType);
                InputDataHandle inputHandle =
                    executorFactory.createInputDataHandle(
                            settings, suggestedInputFile);
                OutputStream out = inputHandle.openInputFileOutStream();
                fileType.writeTable(table.getDataTableSpec(), it,
                        table.getRowCount(), out, subProg);
                out.close();
                Arrays.fill(inputHandles[port], inputHandle);
            }
            assert !it.hasNext() : "Did not process all rows";
            subProg.setProgress(1.0);
        }
        // determine output file names
        for (int port = 0; port < nrOutPorts; port++) {
            AbstractFileTypeRead outputFileType =
                settings.createOutputFileType(port);
            outputFileTypes[port] = outputFileType;
            if (port == 0 && chunkCount > 1) {
                for (int chunkIdx = 0; chunkIdx < chunkCount; chunkIdx++) {
                    File suggestedOutFile = getOutputFilePath(
                            port, chunkIdx, outputFileType);
                    OutputDataHandle handle =
                        executorFactory.createOutputDataHandle(
                                settings, suggestedOutFile);
                    outputHandles[port][chunkIdx] = handle;
                }
            } else {
                File suggestedOutFile = getOutputFilePath(
                        port, -1, outputFileType);
                OutputDataHandle handle =
                    executorFactory.createOutputDataHandle(
                            settings, suggestedOutFile);
                Arrays.fill(outputHandles[port], handle);
            }
        }

        for (int chunk = 0; chunk < chunkCount; chunk++) {
            InputDataHandle[] ins = new InputDataHandle[nrInPorts];
            for (int port = 0; port < nrInPorts; port++) {
                ins[port] = inputHandles[port][chunk];
            }
            OutputDataHandle[] outs = new OutputDataHandle[nrOutPorts];
            for (int port = 0; port < nrOutPorts; port++) {
                outs[port] = outputHandles[port][chunk];
            }
            String[] commandlineArgs = createCommandlineArgs(ins, outs);
            AbstractExttoolExecutor exe = executorFactory.createNewInstance();
            ExecutionChunkCallable callable = createExecutionChunkCallable(exe);
            callable.setInputHandles(ins);
            callable.setOutputHandles(outs);
            callable.setOutputFileTypes(outputFileTypes);
            callable.setCommandlineArgs(commandlineArgs);
            result.add(callable);
        }
        return result;
    }

    /** Main execution called from the node's execute method. It writes the
     * input data, runs the process(es), reads back the results and merges
     * the input with the output data.
     * @param inData The input data of the node.
     * @param exec for progress monitoring, cancelation, table creation.
     * @return The final output table(s).
     * @throws Exception In case of errors.
     */
    public BufferedDataTable[] execute(final BufferedDataTable[] inData,
                final ExecutionContext exec) throws Exception {
        if (inData.length != m_customizer.getNrInputs()) {
            throw new Exception("Invalid input length: " + inData.length);
        }
        ThreadPool threadPool =
            KNIMEConstants.GLOBAL_THREAD_POOL.createSubPool(8);
        double pre = 0.1;
        double main = 0.7;
        double post = 0.1;
        double merge = 0.1;
        exec.setMessage("Writing input");
        ExecutionMonitor subExec = exec.createSubProgress(pre);
        List<ExecutionChunkCallable> executionChunks =
            prepareExecution(inData, subExec);
        final int chunkCount = executionChunks.size();
        subExec.setProgress(1.0);
        ExecutionContext mainExec = exec.createSubExecutionContext(main);
        exec.setMessage("Calling executable (" + chunkCount + " chunk(s))");
        List<Future<BufferedDataTable[]>> futures =
            new ArrayList<Future<BufferedDataTable[]>>();
        AtomicInteger rowUnifier = new AtomicInteger();
        for (final ExecutionChunkCallable ec : executionChunks) {
            final ExecutionContext sub =
                mainExec.createSilentSubExecutionContext(1.0 / chunkCount);
            ec.setExecutionContext(sub);
            if (chunkCount > 0) {
                ec.setRowIdUnifier(rowUnifier);
            }
            futures.add(threadPool.enqueue(ec));
        }
        boolean success = false;
        final int nrOutputs = getCustomizer().getNrOutputs();
        @SuppressWarnings("unchecked")
        List<BufferedDataTable>[] tablesPerPort = new ArrayList[nrOutputs];
        for (int i = 0; i < nrOutputs; i++) {
            tablesPerPort[i] = new ArrayList<BufferedDataTable>();
        }
        List<Throwable> failures = new ArrayList<Throwable>();
        for (int chunk = 0; chunk < chunkCount; chunk++) {
            Future<BufferedDataTable[]> f = futures.get(chunk);
            BufferedDataTable[] result;
            try {
                result = f.get();
                if (result.length != nrOutputs) {
                    IndexOutOfBoundsException iooe =
                        new IndexOutOfBoundsException("Returned table "
                                + "array is not of expected length, got "
                                + result.length + ", expected " + nrOutputs);
                    throw new ExecutionException(iooe);
                }
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (chunkCount == 1) {
                    if (cause instanceof Exception) {
                        throw (Exception)cause;
                    } else {
                        throw e;
                    }
                }
                String message = "Execution on chunk " + chunk + " failed: "
                    + e.getMessage();
                addWarningMessage(message);
                LOGGER.warn(message, cause);
                failures.add(cause);
                continue;
            }
            success = true;
            for (int i = 0; i < tablesPerPort.length; i++) {
                tablesPerPort[i].add(result[i]);
            }
        }
        if (!success) {
            throw new Exception("Failed on all chunks, last error:  "
                    + failures.get(failures.size() - 1).getMessage());
        }
        BufferedDataTable[] finalResult = new BufferedDataTable[nrOutputs];
        if (chunkCount == 1) {
            for (int i = 0; i < nrOutputs; i++) {
                finalResult[i] = tablesPerPort[i].get(0);
            }
        } else {
            exec.setMessage("Aggregating output tables");
            finalResult = new BufferedDataTable[nrOutputs];
            ExecutionContext postExec = exec.createSubExecutionContext(post);
            for (int i = 0; i < nrOutputs; i++) {
                ExecutionMonitor sub =
                    postExec.createSubProgress(1.0 / nrOutputs);
                BufferedDataTable[] fromChunks = tablesPerPort[i].toArray(
                        new BufferedDataTable[tablesPerPort[i].size()]);
                finalResult[i] =
                    postExec.createConcatenateTable(sub, fromChunks);
            }
        }
        if (inData.length == 0 || nrOutputs == 0) {
            return finalResult;
        }
        ExecutionContext mergeExec = exec.createSubExecutionContext(merge);
        BufferedDataTable in = inData[0];
        BufferedDataTable out = finalResult[0];
        String idColumn = out.getDataTableSpec().getColumnSpec(0).getName();
        finalResult[0] = joinInAndOutputTable(in, out, idColumn, mergeExec);
        return finalResult;

    }

    /** Joins the first input data table with the first output data table. The
     * output table must retain the same row-ordering as the input table, though
     * it may contain additional rows in between (with the same IDs) or no
     * matching row at all.
     * @param in The input table.
     * @param out The output table (output from external tool).
     * @param idColInOutTable Name of the column in the output table containing
     *        the ID based on which the matching with the input row IDs is done.
     * @param exec for progress/cancellation/table creation.
     * @return The final output table (at port 0)
     * @throws Exception In case of problems.
     */
    protected BufferedDataTable joinInAndOutputTable(
            final BufferedDataTable in, final BufferedDataTable out,
            final String idColInOutTable, final ExecutionContext exec)
        throws Exception {
        RowIterator inIt = in.iterator();
        DataTableSpec inSpec = in.getDataTableSpec();
        int inCount = in.getRowCount();

        DataTableSpec outSpec = out.getDataTableSpec();
        int idCol = outSpec.findColumnIndex(idColInOutTable);
        if (idCol < 0) {
            throw new Exception("No ID column \"" + idColInOutTable
                    + "\" in output table: " + outSpec);
        }

        HashSet<String> namesHash = new HashSet<String>();
        List<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>();
        for (int i = 0; i < inSpec.getNumColumns(); i++) {
            DataColumnSpec col = inSpec.getColumnSpec(i);
            namesHash.add(col.getName());
            colSpecs.add(col);
        }
        for (int i = 0; i < outSpec.getNumColumns(); i++) {
            if (i != idCol) {
                DataColumnSpec col = outSpec.getColumnSpec(i);
                if (!namesHash.add(col.getName())) {
                    int index = 1;
                    String name;
                    do {
                        name = col.getName() + "(#" + (index++) + ")";
                    } while (!namesHash.add(name));
                    DataColumnSpecCreator c = new DataColumnSpecCreator(col);
                    c.setName(name);
                    colSpecs.add(c.createSpec());
                } else {
                    colSpecs.add(col);
                }
            }
        }
        DataTableSpec spec = new DataTableSpec(inSpec.getName(),
                colSpecs.toArray(new DataColumnSpec[colSpecs.size()]));
        BufferedDataContainer cont = exec.createDataContainer(spec);
        RowIterator outIt = out.iterator();

        DataRow lastFromRight = outIt.hasNext() ? outIt.next() : null;
        int index = 0;
        int runningIndex = 0;
        while (inIt.hasNext()) {
            runningIndex = 0;
            DataRow inRow = inIt.next();
            exec.setProgress(index / (double)inCount,
                    "Finding matches to row \"" + inRow.getKey() + "\" ("
                    + index + "/" + inCount + ")");
            exec.checkCanceled();
            if (lastFromRight == null && outIt.hasNext()) {
                lastFromRight = outIt.next();
            }
            while (addToContainerIfMatches(inRow, lastFromRight,
                    idCol, cont, runningIndex)) {
                runningIndex++;
                lastFromRight = outIt.hasNext() ? outIt.next() : null;
            }
            if (runningIndex == 0) {
                DataCell[] missingCells = new DataCell[outSpec.getNumColumns()];
                Arrays.fill(missingCells, DataType.getMissingCell());
                missingCells[idCol] =
                    new StringCell(inRow.getKey().getString());
                DataRow missingRow =
                    new DefaultRow(inRow.getKey(), missingCells);
                boolean add = addToContainerIfMatches(inRow, missingRow, idCol,
                        cont, runningIndex++);
                assert add : "Fill row was not added";
            }
        }
        exec.setMessage("Filling remaining rows");
        int uniquifier = 1;
        while (lastFromRight != null) {
            exec.checkCanceled();
            DataCell[] inMissingCells = new DataCell[inSpec.getNumColumns()];
            Arrays.fill(inMissingCells, DataType.getMissingCell());
            RowKey key = new RowKey(lastFromRight.getCell(idCol).toString());
            DataRow inMissing = new DefaultRow(key, inMissingCells);
            boolean add = addToContainerIfMatches(inMissing, lastFromRight,
                    idCol, cont, uniquifier++);
            assert add;
            lastFromRight = outIt.hasNext() ? outIt.next() : null;
        }
        cont.close();
        return cont.getTable();
    }

    /** Add a row the output container if the two argument rows match. */
    private boolean addToContainerIfMatches(final DataRow left,
            final DataRow right, final int idColInRight,
            final BufferedDataContainer cont, final int runningIndex) {
        if (left == null || right == null) {
            return false;
        }
        DataCell idCellRight = right.getCell(idColInRight);
        RowKey leftKey = left.getKey();
        if (leftKey.getString().equals(idCellRight.toString())) {
            RowKey newKey = leftKey;
            if (runningIndex > 0) {
                newKey = new RowKey(leftKey + "_" + runningIndex);
            }
            int leftCount = left.getNumCells();
            int rightCount = right.getNumCells();
            DataCell[] newCells = new DataCell[leftCount + rightCount - 1];
            int index = 0;
            for (int i = 0; i < leftCount; i++) {
                newCells[index++] = left instanceof BlobSupportDataRow
                ? ((BlobSupportDataRow)left).getRawCell(i) : left.getCell(i);
            }
            for (int i = 0; i < rightCount; i++) {
                if (i == idColInRight) {
                    continue;
                }
                newCells[index++] = right instanceof BlobSupportDataRow
                ? ((BlobSupportDataRow)right).getRawCell(i) : right.getCell(i);
            }
            cont.addRowToTable(new BlobSupportDataRow(newKey, newCells));
            return true;
        }
        return false;
    }

    /** Creates the complete command line prior execution. It replaces the
     * in- and output place holders by their final actual file paths.
     * @param inputHandles Input handles to the external tool.
     * @param outputHandles Output handles, containing result data.
     * @return The list of final commandline arguments
     * @throws InvalidSettingsException If settings are invalid.
     */
    protected String[] createCommandlineArgs(
            final InputDataHandle[] inputHandles,
            final OutputDataHandle[] outputHandles)
    throws InvalidSettingsException {
        String[] args = m_settings.getCommandlineArgs();
        String[] copy = Arrays.copyOf(args, args.length);
        // replace in each argument %inFile%, %inFile_x%",... by the full paths
        for (int i = 0; i < copy.length; i++) {
            for (int iPort = 0; iPort < m_customizer.getNrInputs(); iPort++) {
                String path = inputHandles[iPort].getLocation();
                if (m_customizer.getNrInputs() > 1) {
                    int lastDot = path.lastIndexOf('.');
                    if (lastDot >= 0) {
                        path = path.substring(0, lastDot) + "_"
                            + iPort + path.substring(lastDot);
                    } else {
                        path = path.concat("_" + iPort);
                    }
                }
                if (iPort == 0) {
                    copy[i] = copy[i].replace("%inFile%", path);
                }
                copy[i] = copy[i].replace("%inFile_" + iPort + "%", path);
            }
            for (int oPort = 0; oPort < m_customizer.getNrOutputs(); oPort++) {
                String path = outputHandles[oPort].getLocation();
                if (m_customizer.getNrOutputs() > 1) {
                    int lastDot = path.lastIndexOf('.');
                    if (lastDot >= 0) {
                        path = path.substring(0, lastDot) + "_"
                        + oPort + path.substring(lastDot);
                    } else {
                        path = path.concat("_" + oPort);
                    }
                }
                if (oPort == 0) {
                    copy[i] = copy[i].replace("%outFile%", path);
                }
                copy[i] = copy[i].replace("%outFile_" + oPort + "%", path);
            }
        }
        return copy;
    }

    /** Get the final path of the input file.
     * @param port Port of interest
     * @param chunkIndex Current chunk information.
     * @param inputFileType Associated file type
     * @return The file of the input.
     * @throws IOException If a file can't be created
     * @throws InvalidSettingsException If settings are invalid.
     */
    protected File getInputFilePath(final int port, final int chunkIndex,
            final AbstractFileTypeWrite inputFileType)
        throws IOException, InvalidSettingsException {
        final ExttoolSettings settings = getSettings();
        final ExttoolCustomizer customizer = getCustomizer();
        final int nrInPorts = customizer.getNrInputs();
        if (port < 0 || port >= nrInPorts) {
            throw new IndexOutOfBoundsException("Invalid port: " + port);
        }
        PathAndTypeConfiguration config = settings.getInputConfig(port);
        String inputFile = config.getPath();
        String suffix = inputFileType.getSuffix();
        if (inputFile == null) { // create working directory
            if (m_workingDirectory == null) {
                createWorkingDirectory();
            }
            inputFile = new File(m_inputDirectory,
                    "port" + port + "." + suffix).getAbsolutePath();
        }
        // if chunking is enabled, generate unique name per row/chunk
        if (chunkIndex > 0) {
            // converts "/tmp/foo_xyz/input/myInput.sdf"
            // into     "/tmp/foo_xyz/input/myInput_1.sdf"
            inputFile = inputFile.replaceAll(
                    "(\\.[^\\.]*)$", "_" + chunkIndex + "$1");
        }
        return new File(inputFile);
    }

    /** Get the final path of the output file (file created by the
     * external tool).
     * @param port Port of interest
     * @param chunkIndex Current chunk information.
     * @param outputFileType Associated file type
     * @return Output file.
     * @throws IOException If a file can't be created
     * @throws InvalidSettingsException If settings are invalid.
     */
    protected File getOutputFilePath(final int port, final int chunkIndex,
            final AbstractFileTypeRead outputFileType)
    throws IOException, InvalidSettingsException {
        final ExttoolSettings settings = getSettings();
        final ExttoolCustomizer customizer = getCustomizer();
        final int nrOutPorts = customizer.getNrOutputs();
        if (port < 0 || port >= nrOutPorts) {
            throw new IndexOutOfBoundsException("Invalid port: " + port);
        }
        PathAndTypeConfiguration config = settings.getOutputConfig(port);
        String outputFile = config.getPath();
        String suffix = outputFileType.getSuffix();
        if (outputFile == null) { // create working directory
            if (m_workingDirectory == null) {
                createWorkingDirectory();
            }
            outputFile = new File(m_outputDirectory,
                    "port" + port + "." + suffix).getAbsolutePath();
        }
        // if chunking is enabled, generate unique name per row/chunk
        if (chunkIndex > 0) {
            // converts "/tmp/foo_xyz/output/myOutput.sdf"
            // into     "/tmp/foo_xyz/output/myOutput_1.sdf"
            outputFile = outputFile.replaceAll(
                    "(\\.[^\\.]*)$", "_" + chunkIndex + "$1");
        }
        return new File(outputFile);
    }

    /** Create a callable representing an execution of a chunk.
     * @param executor The associated executor.
     * @return A new chunk callable.
     */
    protected ExecutionChunkCallable createExecutionChunkCallable(
            final AbstractExttoolExecutor executor) {
        return new ExecutionChunkCallable(executor);
    }

    /** Set (or append) a warning message. Called from the the execute method
     * when something should be reported to the user (warning message on the
     * node). Multiple warning messages are separated by newline...
     * @param message The message to set/append.
     */
    protected void addWarningMessage(final String message) {
        if (message == null || message.length() == 0) {
            return;
        }
        if (m_warningMessageBuilder.length() == 0) {
            m_warningMessageBuilder.append(message);
        } else {
            m_warningMessageBuilder.append('\n').append(message);
        }
    }

    /** Get the aggregated warning messages and reset the internal field.
     * @return the aggregated warning message (or null if none was set)
     */
    public String clearWarningMessage() {
        if (m_warningMessageBuilder.length() == 0) {
            return null;
        }
        String result = m_warningMessageBuilder.toString();
        m_warningMessageBuilder.setLength(0);
        return result;
    }

    /** the temp file will have a time stamp in its name. */
    private static final SimpleDateFormat DATE_FORMAT =
        new SimpleDateFormat("yyyyMMdd");

    /** Create a working directory for all the jobs.
     * @throws IOException If the file can't be created.
     * @throws InvalidSettingsException If settings are invalid.
     */
    protected void createWorkingDirectory()
        throws IOException, InvalidSettingsException {
        final ExttoolSettings settings = getSettings();
        String pathToExecutable = settings.getPathToExecutable();
        if (pathToExecutable == null || pathToExecutable.length() == 0) {
            String[] cmdArgs = settings.getCommandlineArgs();
            if (cmdArgs.length > 0) {
                pathToExecutable = cmdArgs[0];
            }
        }
        String baseName = null;
        if (pathToExecutable != null && pathToExecutable.length() > 0) {
            // this searches for the executable name, omitting
            // - the path (first expression, removing all to the last '/' or '\'
            // - the executable extension (e.g. '.exe'), 2nd replaceAll
            String nameOnly = pathToExecutable.replaceAll("^.*[/\\\\]", "")
                .replaceAll("\\.[^\\.]*$", "");
            // make sure to not have strange chars in there
            nameOnly = nameOnly.replaceAll("[^\\w\\.]", "_");
            if (nameOnly.length() > 0) {
                baseName = nameOnly;
            }
        }
        if (baseName == null) {
            baseName = "exttool";
        }
        baseName = baseName + "_" + DATE_FORMAT.format(new Date()) + "_";
        m_workingDirectory = FileUtil.createTempDir(baseName);
        LOGGER.debug("Using temporary directory "
                + m_workingDirectory.getAbsolutePath());
        m_inputDirectory = new File(m_workingDirectory, "input");
        m_inputDirectory.mkdir();
        m_outputDirectory = new File(m_workingDirectory, "output");
        m_outputDirectory.mkdir();
    }

    /** Helper class that creates a filter view on an argument iterator.
     * This is used to sub-divide the input data into several chunks.
     */
    private static final class ViewRowIterator extends RowIterator {

        private final RowIterator m_it;
        private final int m_maxRowCount;
        private int m_currentRowIndex;

        ViewRowIterator(final RowIterator it, final int maxRowCount) {
            m_it = it;
            m_maxRowCount = maxRowCount;
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            return m_currentRowIndex < m_maxRowCount && m_it.hasNext();
        }

        /** {@inheritDoc} */
        @Override
        public DataRow next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            m_currentRowIndex += 1;
            return m_it.next();
        }
    }

}
