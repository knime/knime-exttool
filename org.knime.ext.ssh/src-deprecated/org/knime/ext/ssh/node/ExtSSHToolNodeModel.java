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
import org.knime.core.util.FileUtil;
import org.knime.ext.ssh.SSHUtil;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;

/**
 *
 * @author Peter Ohl, KNIME AG, Zurich, Switzerland
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
                    FileUtil.createTempFile("ExtSSHNodeInputTable", ".txt");
            exec.checkCanceled();

            CSVWriter csvWriter = null;
            try {
                exec.setMessage("Writing input table to "
                        + "(local) temp CSV file...");
                FileWriterSettings fws = createFileWriterSettings();
                FileWriter inTableWriter = new FileWriter(tmpInFile);
                csvWriter = new CSVWriter(inTableWriter, fws);
                csvWriter.write(inData[0], exec.createSubProgress(0));
            } finally {
                if (csvWriter != null) {
                    csvWriter.close();
                }
            }
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
            tmpInFile.delete();
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
                exec.setMessage("Waiting for remote command to finish");
                while (!execChannel.isClosed()) {
                    exec.checkCanceled();
                    Thread.sleep(500);
                }
                LOGGER.debug("SSH execution finished.");
                exec.checkCanceled();

            } finally {
                if (execChannel != null && execChannel.isConnected()) {
                    execChannel.disconnect();
                }
            }

            exec.setMessage("Transferring result output file...");
            File outTableFile =
                    FileUtil.createTempFile("ExtSSHNodeOutputTable", ".txt");
            LOGGER.debug("ftp getting result file "
                    + m_settings.getRemoteOutputFile() + " to "
                    + outTableFile.getAbsolutePath());
            // fixes problem of delayed appearance of output file
            // (noted specifically when accessing (very) remote machines)
            ftpChannel.disconnect();
            ftpChannel = SSHUtil.createNewSFTPChannel(m_settings, session);
            ftpChannel.get(m_settings.getRemoteOutputFile(), outTableFile
                    .getAbsolutePath());
            LOGGER.debug("ftp get done.");
            exec.checkCanceled();

            exec.setMessage("Deleting remote data files...");
            try {
                ftpChannel.rm(m_settings.getRemoteInputFile());
                LOGGER.debug("ftp removed " + m_settings.getRemoteInputFile());
            } catch (Exception e) {
                LOGGER.warn("Unable to delete remote input file.");
            }
            try {
                if (!m_settings.getRemoteInputFile().equals(
                        m_settings.getRemoteOutputFile())) {
                    ftpChannel.rm(m_settings.getRemoteOutputFile());
                    LOGGER.debug("ftp removed "
                            + m_settings.getRemoteOutputFile());
                }
            } catch (Exception e) {
                LOGGER.warn("Unable to delete remote output file.");
            }
            exec.setMessage("Analyzing result file...");
            FileReaderNodeSettings frns =
                    createFileReaderNodeSettings(outTableFile);
            frns = FileAnalyzer.analyze(frns, exec.createSubProgress(0));

            DataTableSpec tSpec =
                    new DataTableSpec("External SSH Tool output", frns
                            .createDataTableSpec(), new DataTableSpec("empty"));

            FileTable ft =
                    new FileTable(tSpec, frns, exec
                            .createSubExecutionContext(0));
            BufferedDataTable[] result = null;
            try {
                exec.checkCanceled();
                result =
                        exec
                                .createBufferedDataTables(new DataTable[]{ft},
                                        exec);

            } finally {
                outTableFile.delete();
            }
            return result;

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
        return new DataTableSpec[]{null};
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
