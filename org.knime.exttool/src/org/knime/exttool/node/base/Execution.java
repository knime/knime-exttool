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
package org.knime.exttool.node.base;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.knime.core.data.DataRow;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.core.util.ThreadPool;
import org.knime.exttool.executor.AbstractExttoolExecutor;
import org.knime.exttool.executor.AbstractExttoolExecutorFactory;
import org.knime.exttool.executor.InputDataHandle;
import org.knime.exttool.executor.OutputDataHandle;
import org.knime.exttool.filetype.AbstractFileTypeRead;
import org.knime.exttool.filetype.AbstractFileTypeWrite;
import org.knime.exttool.node.base.ExttoolSettings.PathAndTypeConfiguration;

/**
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class Execution {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(Execution.class);

    private final ExttoolCustomizer m_customizer;
    private final ExttoolSettings m_settings;

    private File m_workingDirectory;
    private File m_inputDirectory;
    private File m_outputDirectory;

    protected Execution(final ExttoolCustomizer customizer,
            final ExttoolSettings settings) {
        m_customizer = customizer;
        m_settings = settings;
    }

    /**
     * @return the customizer
     */
    protected ExttoolCustomizer getCustomizer() {
        return m_customizer;
    }

    /**
     * @return the settings
     */
    protected ExttoolSettings getSettings() {
        return m_settings;
    }

    protected List<ExecutionChunkCallable> prepareExecution(
            final BufferedDataTable[] inputTables, final ExecutionMonitor exec)
            throws IOException, InvalidSettingsException, CanceledExecutionException {
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

    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
                final ExecutionContext exec) throws Exception {
            ThreadPool threadPool =
                KNIMEConstants.GLOBAL_THREAD_POOL.createSubPool(8);
            double pre = 0.1;
            double main = 0.8;
            double post = 0.1;
            exec.setMessage("Writing input");
            ExecutionMonitor subExec = exec.createSubProgress(pre);
            List<ExecutionChunkCallable> executionChunks =
                prepareExecution(inData, subExec);
            final int chunkCount = executionChunks.size();
            subExec.setProgress(1.0);
            ExecutionContext mainExec = exec.createSubExecutionContext(main);
            exec.setMessage("Calling executable (" + chunkCount + " chunk(s)");
            List<Future<BufferedDataTable[]>> futures =
                new ArrayList<Future<BufferedDataTable[]>>();
            for (final ExecutionChunkCallable ec : executionChunks) {
                final ExecutionContext sub =
                    mainExec.createSilentSubExecutionContext(1.0 / chunkCount);
                ec.setExecutionContext(sub);
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
                    LOGGER.warn("Execution on chunk " + chunk + " failed: "
                            + e.getMessage(), cause);
                    failures.add(cause);
                    continue;
                }
                if (chunkCount == 1) { // one chunk only
                    return result;
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
            exec.setMessage("Aggregating output tables");
            ExecutionContext postExec = exec.createSubExecutionContext(post);
            BufferedDataTable[] finalResult = new BufferedDataTable[nrOutputs];
            for (int i = 0; i < nrOutputs; i++) {
                ExecutionMonitor sub =
                    postExec.createSubProgress(1.0 / nrOutputs);
                BufferedDataTable[] fromChunks = tablesPerPort[i].toArray(
                        new BufferedDataTable[tablesPerPort[i].size()]);
                finalResult[i] =
                    postExec.createConcatenateTable(sub, fromChunks);
            }
            return finalResult;
        }

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

    protected ExecutionChunkCallable createExecutionChunkCallable(
            final AbstractExttoolExecutor executor) {
        return new ExecutionChunkCallable(executor);
    }

    /** the temp file will have a time stamp in its name. */
    private static final SimpleDateFormat DATE_FORMAT =
        new SimpleDateFormat("yyyyMMdd");

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

    private static final class ViewRowIterator extends RowIterator {

        private final RowIterator m_it;
        private final int m_maxRowCount;
        private int m_currentRowIndex;

        /**
         *
         */
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
