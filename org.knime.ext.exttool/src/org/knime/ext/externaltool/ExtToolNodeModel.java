/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 *
 * History
 *   12.10.2006 (ohl): created
 */
package org.knime.ext.externaltool;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

import org.knime.base.node.io.csvwriter.CSVWriter;
import org.knime.base.node.io.csvwriter.FileWriterSettings;
import org.knime.base.node.io.filereader.FileAnalyzer;
import org.knime.base.node.io.filereader.FileReaderNodeSettings;
import org.knime.base.node.io.filereader.FileReaderSettings;
import org.knime.base.node.io.filereader.FileTable;
import org.knime.base.node.io.filereader.SettingsStatus;
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
import org.knime.core.node.property.hilite.DefaultHiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * Implements a node that launches an external executable. Most of its
 * implementation reuses code from the {@link org.knime.base.node.util.exttool}
 * package. The classes in there provide functionality to execute an external
 * tool, to catch its output, and to display it in NodeViews.<br>
 * This part takes care of user settings (locations of files and arguments), and
 * of writing out the data and reading the results back in.
 *
 * @author ohl, University of Konstanz
 */
public class ExtToolNodeModel extends ExtToolOutputNodeModel implements
        Observer {
    /**
     * the maximum number of lines stored for stdout and stderr output of the
     * external tool. (Keeping default scope so views and buffers here have the
     * same length.)
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

    private final HiLiteHandler m_hiliter = new DefaultHiLiteHandler();

    /**
     * A constructor to construct a new instance.
     */
    public ExtToolNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

        // we assume that if the file is not null it is good to use.
        if (m_extExecutable == null) {
            throw new InvalidSettingsException("No external executable "
                    + "specified.");
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
            throw new InvalidSettingsException("No outfile separator "
                    + "specified");
        }

        if (m_inFile.exists() && m_outFile.exists()) {
            setWarningMessage("Existing input and output files will be "
                    + "overridden");
        }
        if (m_inFile.exists()) {
            setWarningMessage("Existing input file will be overridden!");
        }
        if (m_outFile.exists()) {
            setWarningMessage("Existing output file will be overridden!");
        }

        // we don't know what kind of data table will be in the output file
        return new DataTableSpec[]{null};

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        // blow away the output of any previous (failed) runs
        setFailedExternalErrorOutput(new LinkedList<String>());
        setFailedExternalOutput(new LinkedList<String>());

        // clear the views
        notifyViews(null); // null clears.

        // create directories for in and out files
        exec.setProgress("Creating data directory");
        File inParent = m_inFile.getParentFile();
        File outParent = m_outFile.getParentFile();
        if (!inParent.exists()) {
            if (!inParent.mkdirs()) {
                throw new IllegalStateException("Couldn't create directory"
                        + " for input file.");
            }
        }
        if (!outParent.exists()) {
            if (!outParent.mkdirs()) {
                throw new IllegalStateException("Couldn't create directory"
                        + " for output file.");
            }
        }

        // writing input file
        exec.setProgress("Writing data to input file");

        FileWriterSettings settings = new FileWriterSettings();

        String escInSep = FileReaderSettings.unescapeString(m_inSeparator);
        settings.setColSeparator(escInSep);
        settings.setWriteColumnHeader(m_includeColHdr);
        settings.setWriteRowID(m_includeRowHdr);

        CSVWriter csvWriter = new CSVWriter(new FileWriter(m_inFile), settings);

        csvWriter.write(inData[0], exec);
        csvWriter.close();

        String cmdString = m_extExecutable.getAbsolutePath();
        if ((m_extExecArgs != null) && (m_extExecArgs.length() > 0)) {
            cmdString += " " + m_extExecArgs;
        }
        CommandExecution cmdExec = new CommandExecution(cmdString);
        cmdExec.setExecutionDir(m_extCwd);
        cmdExec.addObserver(this);

        try {
            int exitVal = cmdExec.execute(exec);
            if (exitVal != 0) {
                throw new IllegalStateException("Execution failed (exit code "
                        + exitVal + ")");
            }
        } catch (Exception e) {
            // before we return, we save the output in the failing list
            setFailedExternalOutput(new LinkedList<String>(cmdExec
                    .getStdOutput()));
            setFailedExternalErrorOutput(new LinkedList<String>(cmdExec
                    .getStdErr()));
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

    private BufferedDataTable readOutputFile(final ExecutionContext exec)
            throws Exception {
        if (!(m_outFile.exists() && m_outFile.isFile())) {
            throw new IllegalStateException("External executable '"
                    + m_extExecutable.getName() + " didn't produce any output"
                    + " at the specified location ('"
                    + m_outFile.getAbsolutePath() + "')");
        }

        // prepare the settings for the file analyzer
        FileReaderNodeSettings settings = new FileReaderNodeSettings();
        String escOutSep = FileReaderSettings.unescapeString(m_outSeparator);
        settings.addDelimiterPattern(escOutSep, false, false, false);
        settings.addRowDelimiter("\n", true);
        settings.addQuotePattern("\"", "\"");
        settings.setCommentUserSet(true);
        settings.setDataFileLocationAndUpdateTableName(m_outFile.toURI()
                .toURL());
        settings.setTableName(m_extExecutable.getName() + " output");
        settings.setDelimiterUserSet(true);
        settings.setFileHasColumnHeaders(m_hasColHdr);
        settings.setFileHasColumnHeadersUserSet(true);
        settings.setFileHasRowHeaders(m_hasRowHdr);
        settings.setFileHasRowHeadersUserSet(true);
        settings.setQuoteUserSet(true);
        settings.setWhiteSpaceUserSet(true);

        settings = FileAnalyzer.analyze(settings);
        SettingsStatus status = settings.getStatusOfSettings();
        if (status.getNumOfErrors() > 0) {
            throw new IllegalStateException(status.getErrorMessage(0));
        }

        FileTable fTable =
                new FileTable(settings.createDataTableSpec(), settings, null);
        return exec.createBufferedDataTable(fTable, exec);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        super.loadInternals(nodeInternDir, exec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        readSettings(settings, true);
    }

    /**
     * @see org.knime.core.node.NodeModel#reset()
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
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
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
            settings.addString(CFGKEY_EXTTOOLPATH, m_extExecutable
                    .getAbsolutePath());
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
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        readSettings(settings, false);
    }

    /**
     * Reads the settings from the settings object, validates them and if they
     * are all valid - and the takeOver flag is set true - it assignes them to
     * the internal variables.
     *
     * @param settings the object with the settings.
     * @param takeOver if true, and all settings are valid, the values will be
     *            taken over into the internal variables, if false, the settings
     *            are validated only.
     * @throws InvalidSettingsException if the settings are invalid,
     *             inconsistent, or in any way not acceptable.
     */
    private void readSettings(final NodeSettingsRO settings,
            final boolean takeOver) throws InvalidSettingsException {
        String tmpExecFileName; // the external executable
        try {
            tmpExecFileName = settings.getString(CFGKEY_EXTTOOLPATH);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException("The path to the external"
                    + " executable is not specified");
        }
        if ((tmpExecFileName == null) || (tmpExecFileName.length() == 0)) {
            throw new InvalidSettingsException("The path to the external "
                    + "executable is not valid (empty).");
        }
        File tmpExecFile = new File(tmpExecFileName);
        if (!tmpExecFile.exists()) {
            throw new InvalidSettingsException("The specified external "
                    + "executable ('" + tmpExecFileName + "') doesn't exist.");
        }
        if (tmpExecFile.isDirectory()) {
            throw new InvalidSettingsException("The external exec ('"
                    + tmpExecFileName + "') must not be a directory");
        }
        // the working directory for the exec tool
        String tmpExecCwdString = settings.getString(CFGKEY_EXTTOOLCWD, "");
        File tmpExecCwd;
        if (tmpExecCwdString.length() > 0) {
            tmpExecCwd = new File(tmpExecCwdString);
            if (!tmpExecCwd.isDirectory()) {
                throw new InvalidSettingsException(
                        "Specified working directory" + "('" + tmpExecCwdString
                                + "') isn't a directory or doesn't exist.");
            }
        } else {
            // if no cwd was specified we use the executables dir
            tmpExecCwd = tmpExecFile.getParentFile();
            assert tmpExecCwd.isDirectory();
        }
        // the commandline arguments for the external executable
        String tmpExecArgs = settings.getString(CFGKEY_EXTTOOLARGS, "");
        // the temporary input file
        String tmpInFileName;
        try {
            tmpInFileName = settings.getString(CFGKEY_INFILENAME);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException("Inputfilename is not "
                    + "specified");
        }
        if ((tmpInFileName == null) || (tmpInFileName.length() == 0)) {
            throw new InvalidSettingsException("Inputfilename is not "
                    + "valid (empty).");
        }
        File tmpInFile = new File(tmpInFileName);
        if (tmpInFile.isDirectory()) {
            throw new InvalidSettingsException("Input filename must not be a"
                    + " directory");
        }
        // temporary output file
        String tmpOutFileName;
        try {
            tmpOutFileName = settings.getString(CFGKEY_OUTFILENAME);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException("Outputfilename is not "
                    + "specified");
        }
        if ((tmpOutFileName == null) || (tmpOutFileName.length() == 0)) {
            throw new InvalidSettingsException("Outputfilename is not "
                    + "valid (empty).");
        }
        File tmpOutFile = new File(tmpOutFileName);
        if (tmpOutFile.isDirectory()) {
            throw new InvalidSettingsException("Output filename ('"
                    + tmpOutFileName + "') must not be a directory");
        }
        // inSeparator
        String tmpInSeparator;
        try {
            tmpInSeparator = settings.getString(CFGKEY_INSEPARATOR);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException("The separator for the input"
                    + " file is not specified");
        }
        if ((tmpInSeparator == null) || (tmpInSeparator.length() < 1)) {
            throw new InvalidSettingsException("The separator for the input"
                    + " file is invalid (empty)");
        }
        // replace \t \\, etc.
        String escInSep = FileReaderSettings.unescapeString(tmpInSeparator);
        if (escInSep.length() > 1) {
            throw new InvalidSettingsException("The separator for the input"
                    + " file can only be one character.");
        }
        // outSeparator
        String tmpOutSeparator;
        try {
            tmpOutSeparator = settings.getString(CFGKEY_OUTSEPARATOR);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException("The separator of the output"
                    + " file is not specified");
        }
        if ((tmpOutSeparator == null) || (tmpOutSeparator.length() < 1)) {
            throw new InvalidSettingsException("The separator of the output"
                    + " file is invalid (empty)");
        }
        // the column/row header flags
        boolean tmpInclColHdr;
        boolean tmpInclRowHdr;
        boolean tmpHasColHdr;
        boolean tmpHasRowHdr;
        try {
            tmpInclColHdr = settings.getBoolean(CFGKEY_INCLCOLHDR);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException("Format of the temp input"
                    + " file not fully specified (incl. col headers).");
        }
        try {
            tmpInclRowHdr = settings.getBoolean(CFGKEY_INCLROWHDR);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException("Format of the temp input"
                    + " file not fully specified (incl. row IDs).");
        }
        try {
            tmpHasColHdr = settings.getBoolean(CFGKEY_HASCOLHDR);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException("Format of the temp output"
                    + " file not fully specified (has col headers).");
        }
        try {
            tmpHasRowHdr = settings.getBoolean(CFGKEY_HASROWHDR);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException("Format of the temp output"
                    + " file not fully specified (has row IDs).");
        }
        // all settings are alright, so far.
        if (takeOver) {
            m_inFile = tmpInFile;
            m_inSeparator = tmpInSeparator;
            m_includeColHdr = tmpInclColHdr;
            m_includeRowHdr = tmpInclRowHdr;

            m_extExecutable = tmpExecFile;
            m_extCwd = tmpExecCwd;
            m_extExecArgs = tmpExecArgs;

            m_outFile = tmpOutFile;
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
