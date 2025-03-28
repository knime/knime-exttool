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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 */
package org.knime.ext.exttool;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

import org.apache.commons.lang3.StringUtils;
import org.knime.base.node.io.csvwriter.CSVWriter;
import org.knime.base.node.io.csvwriter.FileWriterSettings;
import org.knime.base.node.io.filereader.FileAnalyzer;
import org.knime.base.node.io.filereader.FileReaderNodeSettings;
import org.knime.base.node.io.filereader.FileTable;
import org.knime.base.node.util.exttool.CommandExecution;
import org.knime.base.node.util.exttool.ExtToolOutputNodeModel;
import org.knime.base.node.util.exttool.ViewUpdateNotice;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortType;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.tokenizer.SettingsStatus;
import org.knime.core.util.tokenizer.TokenizerSettings;

/**
 * Implements a node that launches an external executable. Most of its implementation reuses code from the
 * {@link org.knime.base.node.util.exttool} package. The classes in there provide functionality to execute an external
 * tool, to catch its output, and to display it in NodeViews.<br>
 * This part takes care of user settings (locations of files and arguments), and of writing out the data and reading the
 * results back in.
 *
 * @author ohl, University of Konstanz
 */
public class ExtToolNodeModel extends ExtToolOutputNodeModel implements Observer {
    /**
     * the maximum number of lines stored for stdout and stderr output of the external tool. (Keeping default scope so
     * views and buffers here have the same length.)
     */
    static final int MAX_OUTLINES_STORED = 500;

    /**
     * Key used to store setting value in config object.
     */
    static final String CFGKEY_HASROWHDR = "ContainsRowHdr";

    /**
     * Key used to store setting value in config object.
     */
    static final String CFGKEY_HASCOLHDR = "ContainsColHdr";

    /**
     * Key used to store setting value in config object.
     */
    static final String CFGKEY_OUTSEPARATOR = "Outputfile separator";

    /**
     * Key used to store setting value in config object.
     */
    static final String CFGKEY_OUTFILENAME = "Output filename";

    /**
     * Key used to store setting value in config object.
     */
    static final String CFGKEY_EXTTOOLARGS = "ExtExecArgs";

    /**
     * Key used to store setting value in config object.
     */
    static final String CFGKEY_EXTTOOLCWD = "ExtExecWorkDir";

    /**
     * Key used to store setting value in config object.
     */
    static final String CFGKEY_EXTTOOLPATH = "PathToExtExec";

    /**
     * Key used to store setting value in config object.
     */
    static final String CFGKEY_INCLROWHDR = "IncludeRowHdr";

    /**
     * Key used to store setting value in config object.
     */
    static final String CFGKEY_INCLCOLHDR = "IncludeColHdr";

    /**
     * Key used to store setting value in config object.
     */
    static final String CFGKEY_INSEPARATOR = "Inputfile separator";

    /**
     * Key used to store setting value in config object.
     */
    static final String CFGKEY_INFILENAME = "Inputfile name";

    private File m_inFile;

    private String m_inSeparator;

    private boolean m_includeColHdr;

    private boolean m_includeRowHdr;

    private File m_outFile;

    private String m_outSeparator;

    private boolean m_hasColHdr;

    private boolean m_hasRowHdr;

    private File m_extExecutable;

    private File m_extCwd;

    private String m_extExecArgs;

    private final HiLiteHandler m_hiliter = new HiLiteHandler();

    /**
     * A constructor to construct a new instance.
     */
    public ExtToolNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

        // we assume that if the file is not null it is good to use.
        if (m_extExecutable == null) {
            throw new InvalidSettingsException("No external executable specified.");
        }
        if (m_inFile == null) {
            throw new InvalidSettingsException("No input filename specified.");
        }
        if (m_outFile == null) {
            throw new InvalidSettingsException("No output filename specified.");
        }
        if ((m_inSeparator == null) || (m_inSeparator.length() < 1)) {
            throw new InvalidSettingsException("No infile separator specified");
        }
        if ((m_outSeparator == null) || (m_outSeparator.length() < 1)) {
            throw new InvalidSettingsException("No outfile separator specified");
        }

        if (Files.exists(m_inFile.toPath()) && Files.exists(m_outFile.toPath())) {
            setWarningMessage("Existing input and output files will be overridden");
        } else if (Files.exists(m_inFile.toPath())) {
            setWarningMessage("Existing input file will be overridden!");
        } else if (Files.exists(m_outFile.toPath())) {
            setWarningMessage("Existing output file will be overridden!");
        }

        // we don't know what kind of data table will be in the output file
        return new DataTableSpec[]{null};

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        validatePaths(m_extExecutable.toPath(), m_extCwd.toPath(), m_inFile.toPath(), m_outFile.toPath());

        // blow away the output of any previous (failed) runs
        setFailedExternalErrorOutput(new LinkedList<>());
        setFailedExternalOutput(new LinkedList<>());

        // clear the views
        notifyViews(null); // null clears.

        // create directories for in and out files
        exec.setProgress("Creating data directory");
        File inParent = m_inFile.getParentFile();
        File outParent = m_outFile.getParentFile();
        CheckUtils.checkState(inParent.exists() || inParent.mkdirs(), "Couldn't create directory for input file.");
        CheckUtils.checkState(outParent.exists() || outParent.mkdirs(), "Couldn't create directory for output file.");

        // writing input file
        exec.setProgress("Writing data to input file");

        FileWriterSettings settings = new FileWriterSettings();

        String escInSep = TokenizerSettings.unescapeString(m_inSeparator);
        settings.setColSeparator(escInSep);
        settings.setWriteColumnHeader(m_includeColHdr);
        settings.setWriteRowID(m_includeRowHdr);

        try (FileWriter fileWriter = new FileWriter(m_inFile, StandardCharsets.UTF_8);
                CSVWriter csvWriter = new CSVWriter(fileWriter, settings)) {
            csvWriter.write(inData[0], exec);
        }

        final String[] cmdandargs;
        if ((m_extExecArgs != null) && (m_extExecArgs.length() > 0)) {
            final var args = m_extExecArgs.split("[ \t\f\n\r]+"); // NOSONAR: easier to read and only called once/execution
            cmdandargs = new String[args.length + 1];
            cmdandargs[0] = m_extExecutable.getAbsolutePath();
            System.arraycopy(args, 0, cmdandargs, 1, args.length);
        } else {
            cmdandargs = new String[] {m_extExecutable.getAbsolutePath()};
        }
        final var cmdExec = new CommandExecution(cmdandargs);
        cmdExec.setExecutionDir(m_extCwd);
        cmdExec.addObserver(this);

        try {
            int exitVal = cmdExec.execute(exec);
            if (exitVal != 0) {
                throw new IllegalStateException("Execution failed (exit code " + exitVal + ")");
            }
        } catch (Exception e) {
            // before we return, we save the output in the failing list
            setFailedExternalOutput(new LinkedList<String>(cmdExec.getStdOutput()));
            setFailedExternalErrorOutput(new LinkedList<String>(cmdExec.getStdErr()));
            throw e;
        }

        // on successful execution preserve the output
        setExternalErrorOutput(new LinkedList<String>(cmdExec.getStdErr()));
        setExternalOutput(new LinkedList<String>(cmdExec.getStdOutput()));

        exec.setProgress("Reading data from output file");
        BufferedDataTable outTable = readOutputFile(exec);

        cmdExec.deleteObserver(this);
        return new BufferedDataTable[]{outTable};

    }

    private BufferedDataTable readOutputFile(final ExecutionContext exec) throws Exception {
        if (!(m_outFile.exists() && m_outFile.isFile())) {
            throw new IllegalStateException("External executable '" + m_extExecutable.getName()
                + " didn't produce any output" + " at the specified location ('" + m_outFile.getAbsolutePath() + "')");
        }

        // prepare the settings for the file analyzer
        FileReaderNodeSettings settings = new FileReaderNodeSettings();
        String escOutSep = TokenizerSettings.unescapeString(m_outSeparator);
        settings.addDelimiterPattern(escOutSep, false, false, false);
        settings.addRowDelimiter("\n", true);
        settings.addQuotePattern("\"", "\"");
        settings.setCommentUserSet(true);
        settings.setDataFileLocationAndUpdateTableName(m_outFile.toURI().toURL());
        settings.setDelimiterUserSet(true);
        settings.setFileHasColumnHeaders(m_hasColHdr);
        settings.setFileHasColumnHeadersUserSet(true);
        settings.setFileHasRowHeaders(m_hasRowHdr);
        settings.setFileHasRowHeadersUserSet(true);
        settings.setQuoteUserSet(true);
        settings.setWhiteSpaceUserSet(true);

        settings = FileAnalyzer.analyze(settings, null);
        SettingsStatus status = settings.getStatusOfSettings();
        if (status.getNumOfErrors() > 0) {
            throw new IllegalStateException(status.getErrorMessage(0));
        }

        FileTable fTable = new FileTable(settings.createDataTableSpec(), settings, null);
        return exec.createBufferedDataTable(fTable, exec);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        super.loadInternals(nodeInternDir, exec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        readSettings(settings, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        super.reset();
        // clear all hilites at reset.
        m_hiliter.fireClearHiLiteEvent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        super.saveInternals(nodeInternDir, exec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_inFile != null) {
            settings.addString(CFGKEY_INFILENAME, m_inFile.getAbsolutePath());
        }
        if (m_outFile != null) {
            settings.addString(CFGKEY_OUTFILENAME, m_outFile.getAbsolutePath());
        }
        if (m_extExecutable != null) {
            settings.addString(CFGKEY_EXTTOOLPATH, m_extExecutable.getAbsolutePath());
        }
        if (m_extCwd != null) {
            settings.addString(CFGKEY_EXTTOOLCWD, m_extCwd.getAbsolutePath());
        }
        if (m_extExecArgs != null) {
            settings.addString(CFGKEY_EXTTOOLARGS, m_extExecArgs);
        }
        if (m_inSeparator != null) {
            settings.addString(CFGKEY_INSEPARATOR, m_inSeparator);
        }
        if (m_outSeparator != null) {
            settings.addString(CFGKEY_OUTSEPARATOR, m_outSeparator);
        }
        settings.addBoolean(CFGKEY_INCLCOLHDR, m_includeColHdr);
        settings.addBoolean(CFGKEY_INCLROWHDR, m_includeRowHdr);
        settings.addBoolean(CFGKEY_HASCOLHDR, m_hasColHdr);
        settings.addBoolean(CFGKEY_HASROWHDR, m_hasRowHdr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        readSettings(settings, false);
    }

    static void validatePaths(final Path exec, final Path execPWD, final Path in, final Path out)
        throws InvalidSettingsException {
        CheckUtils.checkSetting(Files.exists(exec), "The specified external executable ('%s') doesn't exist.", exec);
        CheckUtils.checkSetting(!Files.isDirectory(exec),
            "The specified external executable ('%s') must no be a directory.", exec);
        CheckUtils.checkSetting(Files.isDirectory(execPWD),
            "Specified working directory ('%s') isn't a directory or doesn't exist.", execPWD);

        CheckUtils.checkSetting(!Files.isDirectory(in), "Input filename ('%s') must no be a directory.", in);
        CheckUtils.checkSetting(!Files.isDirectory(out), "Output filename ('%s') must no be a directory.", out);
    }

    /**
     * Reads the settings from the settings object, validates them and if they are all valid - and the takeOver flag is
     * set true - it assignes them to the internal variables.
     *
     * @param settings the object with the settings.
     * @param takeOver if true, and all settings are valid, the values will be taken over into the internal variables,
     *            if false, the settings are validated only.
     * @throws InvalidSettingsException if the settings are invalid, inconsistent, or in any way not acceptable.
     */
    private void readSettings(final NodeSettingsRO settings, final boolean takeOver) throws InvalidSettingsException {
        final String tmpExecFileName; // the external executable
        try {
            tmpExecFileName = settings.getString(CFGKEY_EXTTOOLPATH);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException("The path to the external executable is not specified", ise);
        }
        CheckUtils.checkSetting(StringUtils.isNotEmpty(tmpExecFileName),
            "The path to the external executable is not valid (empty).");
        // the working directory for the exec tool
        String tmpExecCwdString = settings.getString(CFGKEY_EXTTOOLCWD, "");
        if (StringUtils.isEmpty(tmpExecCwdString)) {
            // if no cwd was specified we use the executables dir
            tmpExecCwdString = new File(tmpExecFileName).getParentFile().toString();
        }
        // the commandline arguments for the external executable
        final var tmpExecArgs = settings.getString(CFGKEY_EXTTOOLARGS, "");
        // the temporary input file
        final String tmpInFileName;
        try {
            tmpInFileName = settings.getString(CFGKEY_INFILENAME);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException("Input filename is not specified", ise);
        }
        CheckUtils.checkSetting(StringUtils.isNotEmpty(tmpInFileName), "Input filename is not valid (empty).");
        // temporary output file
        final String tmpOutFileName;
        try {
            tmpOutFileName = settings.getString(CFGKEY_OUTFILENAME);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException("Output filename is not specified", ise);
        }
        CheckUtils.checkSetting(StringUtils.isNotEmpty(tmpOutFileName), "Output filename is not valid (empty).");
        // inSeparator
        final String tmpInSeparator;
        try {
            tmpInSeparator = settings.getString(CFGKEY_INSEPARATOR);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException("The separator for the input file is not specified", ise);
        }
        CheckUtils.checkSetting(StringUtils.isNotEmpty(tmpInSeparator),
            "The separator for the input file is invalid (empty)");
        // replace \t \\, etc.
        final var escInSep = TokenizerSettings.unescapeString(tmpInSeparator);
        CheckUtils.checkSetting(escInSep.length() == 1, "The separator for the input file can only be one character.");
        // outSeparator
        final String tmpOutSeparator;
        try {
            tmpOutSeparator = settings.getString(CFGKEY_OUTSEPARATOR);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException("The separator of the output file is not specified", ise);
        }
        CheckUtils.checkSetting(StringUtils.isNotEmpty(tmpOutSeparator),
            "The separator of the output file is invalid (empty)");
        // the column/row header flags
        boolean tmpInclColHdr;
        boolean tmpInclRowHdr;
        boolean tmpHasColHdr;
        boolean tmpHasRowHdr;
        try {
            tmpInclColHdr = settings.getBoolean(CFGKEY_INCLCOLHDR);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException(
                "Format of the temp input file not fully specified (incl. col headers).", ise);
        }
        try {
            tmpInclRowHdr = settings.getBoolean(CFGKEY_INCLROWHDR);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException(
                "Format of the temp input file not fully specified (incl. row IDs).", ise);
        }
        try {
            tmpHasColHdr = settings.getBoolean(CFGKEY_HASCOLHDR);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException(
                "Format of the temp output file not fully specified (has col headers).", ise);
        }
        try {
            tmpHasRowHdr = settings.getBoolean(CFGKEY_HASROWHDR);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException(
                "Format of the temp output file not fully specified (has row IDs).", ise);
        }
        // all settings are alright, so far.
        if (takeOver) {
            m_inFile = new File(tmpInFileName);
            m_inSeparator = tmpInSeparator;
            m_includeColHdr = tmpInclColHdr;
            m_includeRowHdr = tmpInclRowHdr;

            m_extExecutable = new File(tmpExecFileName);
            m_extCwd = new File(tmpExecCwdString);
            m_extExecArgs = tmpExecArgs;

            m_outFile = new File(tmpOutFileName);
            m_outSeparator = tmpOutSeparator;
            m_hasColHdr = tmpHasColHdr;
            m_hasRowHdr = tmpHasRowHdr;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        if (outIndex == 0) {
            return m_hiliter;
        }
        return super.getOutHiLiteHandler(outIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final Observable o, final Object arg) {
        if (arg instanceof ViewUpdateNotice) {
            notifyViews(arg);
        }
    }
}
