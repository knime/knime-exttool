/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jul 13, 2009 (ohl): created
 */
package org.knime.ext.ssh.node;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;

import org.knime.base.node.io.csvwriter.CSVWriter;
import org.knime.base.node.io.csvwriter.FileWriterSettings;
import org.knime.base.node.io.csvwriter.FileWriterSettings.quoteMode;
import org.knime.base.node.io.filereader.FileAnalyzer;
import org.knime.base.node.io.filereader.FileReaderNodeSettings;
import org.knime.base.node.io.filereader.FileTable;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.ext.ssh.SSHUtil;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;

/**
 *
 * @author Peter Ohl, KNIME.com, Zurich, Switzerland
 */
public class ExtSSHToolNodeModel extends NodeModel {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(ExtSSHToolNodeModel.class);

    private ExtSSHToolSettings m_settings = new ExtSSHToolSettings();

    /**
     *
     */
    public ExtSSHToolNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings = new ExtSSHToolSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to reset
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new ExtSSHToolSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        Session session = null;
        ChannelSftp ftpChannel = null;
        try {
            session = SSHUtil.getConnectedSession(m_settings);

            exec.setMessage("Opening ftp connection...");
            ftpChannel = SSHUtil.createNewSFTPChannel(m_settings, session);
            exec.checkCanceled();
            File tmpInFile =
                    File.createTempFile("ExtSSHNodeInputTable", ".txt");
            exec.checkCanceled();

            exec.setMessage("Writing input table to (local) temp CSV file...");
            FileWriterSettings fws = createFileWriterSettings();
            FileWriter inTableWriter = new FileWriter(tmpInFile);
            CSVWriter csvWriter = new CSVWriter(inTableWriter, fws);
            csvWriter.write(inData[0], exec.createSubProgress(0));
            LOGGER.debug("Wrote input table to " + tmpInFile.getAbsolutePath());
            exec.checkCanceled();

            exec.setMessage("Creating remote input directory...");
            File remoteInFile = new File(m_settings.getRemoteInputFile());
            SSHUtil.ftpMkdirs(ftpChannel, remoteInFile.getParent());
            LOGGER.debug("Made remote directories " + remoteInFile.getParent());
            exec.checkCanceled();

            exec.setMessage("Transferring input table csv-file...");
            LOGGER.debug("Starting ftp of " + tmpInFile.getAbsolutePath()
                    + " to " + m_settings.getRemoteInputFile());
            ftpChannel.put(tmpInFile.getAbsolutePath(), m_settings
                    .getRemoteInputFile());
            LOGGER.debug("ftp put done.");
            exec.checkCanceled();

            exec.setMessage("Preparing for execution...");
            LOGGER.debug("Opening Exec channel");
            ChannelExec execChannel = (ChannelExec)session.openChannel("exec");
            try {
                String cmd = m_settings.getCommand();
                // replace $inFile and $outFile with paths
                cmd =
                        ExtSSHToolSettings.replaceTempFileVariables(cmd,
                                m_settings);
                execChannel.setCommand(cmd);
                execChannel.setErrStream(System.err);
                execChannel.setOutputStream(System.out);
                // once more before take-off
                exec.checkCanceled();
                exec.setMessage("Executing on host "
                        + m_settings.getRemoteHost());
                LOGGER.info("Executing node via SSH on "
                        + m_settings.getRemoteHost());
                LOGGER.debug("Executing remotely command: '" + cmd + "'");
                execChannel.connect(m_settings.getTimeoutMilliSec());
                LOGGER.debug("SSH execution finished.");
                exec.checkCanceled();

            } finally {
                if (execChannel != null && execChannel.isConnected()) {
                    execChannel.disconnect();
                }
            }

            exec.setMessage("Transferring result output file...");
            File outTableFile =
                    File.createTempFile("ExtSSHNodeOutputTable", ".txt");
            LOGGER.debug("ftp getting result file "
                    + m_settings.getRemoteOutputFile() + " to "
                    + outTableFile.getAbsolutePath());
            ftpChannel.get(m_settings.getRemoteOutputFile(), outTableFile
                    .getAbsolutePath());
            LOGGER.debug("ftp get done.");
            exec.checkCanceled();

            exec.setMessage("Deleting remote data files...");
            ftpChannel.rm(m_settings.getRemoteInputFile());
            LOGGER.debug("ftp removed " + m_settings.getRemoteInputFile());
            ftpChannel.rm(m_settings.getRemoteOutputFile());
            LOGGER.debug("ftp removed " + m_settings.getRemoteOutputFile());

            exec.setMessage("Analyzing result file...");
            FileReaderNodeSettings frns =
                    createFileReaderNodeSettings(outTableFile);
            frns = FileAnalyzer.analyze(frns, exec.createSubProgress(0));

            FileTable ft =
                    new FileTable(frns.createDataTableSpec(), frns, exec
                            .createSubExecutionContext(0));
            exec.checkCanceled();

            return exec.createBufferedDataTables(new DataTable[]{ft}, exec);

        } catch (Exception e) {
            if ((!(e instanceof CanceledExecutionException))
                    && e.getMessage() != null && !e.getMessage().isEmpty()) {
                LOGGER.error("Job submission failed: " + e.getMessage());
            }
            throw e;

        } finally {
            if (ftpChannel != null && ftpChannel.isConnected()) {
                ftpChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }

    }

    private FileWriterSettings createFileWriterSettings() {
        FileWriterSettings fws = new FileWriterSettings();
        fws.setColSeparator(",");
        fws.setMissValuePattern("");
        fws.setQuoteBegin("\"");
        fws.setQuoteEnd("\"");
        fws.setQuoteMode(quoteMode.IF_NEEDED);
        fws.setWriteColumnHeader(true);
        fws.setWriteRowID(true);
        return fws;
    }

    private FileReaderNodeSettings createFileReaderNodeSettings(final File f)
            throws MalformedURLException {
        FileReaderNodeSettings frs = new FileReaderNodeSettings();
        frs.setDataFileLocationAndUpdateTableName(f.toURI().toURL());
        return frs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        String msg = m_settings.getStatusMsg();
        if (msg != null) {
            throw new InvalidSettingsException(msg);
        }
        // we never know what is coming back from the external tool
        return new DataTableSpec[] {null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        // hiliting doesn't work through this node.
        return new HiLiteHandler();
    }
}
