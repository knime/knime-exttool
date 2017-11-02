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
 *   Mar 9, 2010 (wiswedel): created
 */
package org.knime.exttool.chem.filetype.mol2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

import org.knime.chem.types.Mol2Value;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * Configuration object and executor for the Mol2 writer.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class Mol2Writer {

    /** String history identifier,
     * see {@link org.knime.core.node.util.StringHistory}. */
    static final String HISTORY_ID = "mol2_writer";

    /** Config identifier for target column. */
    private static final String CFG_TARGET_COLUMN = "mol2_column";
    /** Config identifier for target file. */
    private static final String CFG_TARGET_FILE = "output_file";
    /** Config identifier for overwrite OK. */
    private static final String CFG_OVERWRITE_OK = "overwriteOK";
    /** Config identifier for replacing molecule name by row ID. */
    private static final String CFG_REPLACE_TITLE_BY_ID = "replaceTitleByRowID";

    private String m_mol2Column;
    private String m_outputFile;
    private boolean m_overwriteOK;
    private boolean m_replaceTitleByRowID;

    private String m_warningMessage;


    /**
     * @return the mol2Column
     */
    public String getMol2Column() {
        return m_mol2Column;
    }

    /**
     * @param mol2Column the mol2Column to set
     */
    public void setMol2Column(final String mol2Column) {
        m_mol2Column = mol2Column;
    }

    /**
     * @return the outputFile
     */
    public String getOutputFile() {
        return m_outputFile;
    }

    /**
     * @param outputFile the outputFile to set
     */
    public void setOutputFile(final String outputFile) {
        m_outputFile = outputFile;
    }

    /**
     * @return the overwriteOK
     */
    public boolean isOverwriteOK() {
        return m_overwriteOK;
    }

    /**
     * @param overwriteOK the overwriteOK to set
     */
    public void setOverwriteOK(final boolean overwriteOK) {
        m_overwriteOK = overwriteOK;
    }

    /**
     * @return the replaceTitleByRowID
     */
    public boolean isReplaceTitleByRowID() {
        return m_replaceTitleByRowID;
    }

    /**
     * @param replaceTitleByRowID the replaceTitleByRowID to set
     */
    public void setReplaceTitleByRowID(final boolean replaceTitleByRowID) {
        m_replaceTitleByRowID = replaceTitleByRowID;
    }

    /** Saves the current configuration to the argument.
     * @param settings To save to.
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_mol2Column != null) {
            settings.addString(CFG_TARGET_COLUMN, m_mol2Column);
            settings.addString(CFG_TARGET_FILE, m_outputFile);
            settings.addBoolean(CFG_OVERWRITE_OK, m_overwriteOK);
            settings.addBoolean(CFG_REPLACE_TITLE_BY_ID, m_replaceTitleByRowID);
        }
    }

    /** Loads the settings in the model, throws exception if settings are
     * incomplete.
     * @param settings To load from.
     * @throws InvalidSettingsException If incomplete.
     */
    public void loadSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_mol2Column = settings.getString(CFG_TARGET_COLUMN);
        m_outputFile = settings.getString(CFG_TARGET_FILE);
        // added in v2.1
        m_overwriteOK = settings.getBoolean(CFG_OVERWRITE_OK, true);
        // added in v2.2
        m_replaceTitleByRowID =
            settings.getBoolean(CFG_REPLACE_TITLE_BY_ID, false);
    }

    /** Loads the settings in the dialog.
     * @param settings To load from.
     * @param inSpec The input table spec.
     * @throws NotConfigurableException If no appropriate column exists in
     * the input table.
     */
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec inSpec) throws NotConfigurableException {
        if (!inSpec.containsCompatibleType(Mol2Value.class)) {
            throw new NotConfigurableException(
                    "Unable to configure, no Mol2 column in input table.");
        }
        m_outputFile = settings.getString(CFG_TARGET_FILE, null);
        m_mol2Column = settings.getString(CFG_TARGET_COLUMN, "");
        m_overwriteOK = settings.getBoolean(CFG_OVERWRITE_OK, false);
        m_replaceTitleByRowID =
            settings.getBoolean(CFG_REPLACE_TITLE_BY_ID, false);
    }

    /** Performs the nodes execute step. It writes the argument table to its
     * destination file.
     * @param tableSpec The spec associated with the iterator
     * @param it The iterator providing the data
     * @param rowCount The number of rows in the table (for progress)
     * @param exec monitor for progress and cancellation.
     * @throws Exception If that fails for any reason (I/O, invalid config, ...)
     */
    public void execute(final DataTableSpec tableSpec, final RowIterator it,
            final int rowCount,
            final ExecutionMonitor exec) throws Exception {
        BufferedWriter outWriter = openWriter();
        String mol2Column = getMol2Column();
        final int colIndex = tableSpec.findColumnIndex(mol2Column);
        final double count = rowCount; // floating point operations
        int i = 0;
        int missingCount = 0;
        while (it.hasNext()) {
            DataRow r = it.next();
            exec.checkCanceled();
            exec.setProgress(i / count,
                    "Writing row " + i + " (\"" + r.getKey() + "\")");
            DataCell c = r.getCell(colIndex);
            if (c.isMissing()) {
                missingCount++;
            } else {
                Mol2Value v = (Mol2Value)c;
                String toString = v.toString();
                if (m_replaceTitleByRowID) {
                    boolean isTitleLine = false;
                    BufferedReader reader =
                        new BufferedReader(new StringReader(toString));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (isTitleLine) {
                            line = r.getKey().getString();
                            isTitleLine = false;
                        } else if (line.startsWith(
                                Mol2Reader.TRIPOS_MOLECULE)) {
                            isTitleLine = true;
                        }
                        outWriter.write(line);
                        outWriter.newLine();
                    }
                } else {
                    outWriter.write(toString);
                }
                outWriter.newLine();
            }
            i++;
        }
        outWriter.close();
        if (missingCount > 0) {
            addWarningMessage("Skipped " + missingCount
                    + " row(s) because of missing values");
        }
    }

    /** Performs the node's configure step, that is, does a validation of the
     * input table and checks file access.
     * @param in The input table.
     * @throws InvalidSettingsException If the configuration is invalid.
     */
    public void configure(final DataTableSpec in)
        throws InvalidSettingsException {
        if (!in.containsCompatibleType(Mol2Value.class)) {
            throw new InvalidSettingsException(
            "No Mol2 column in input table.");
        }
        checkFileAccess();
        String mol2Column = getMol2Column();
        if (mol2Column == null) { // auto - configure
            int mol2ColCount = 0;
            for (int i = 0; i < in.getNumColumns(); i++) {
                DataColumnSpec s = in.getColumnSpec(i);
                if (s.getType().isCompatible(Mol2Value.class)) {
                    if (mol2Column == null) {
                        mol2Column = in.getColumnSpec(i).getName();
                    }
                }
                mol2ColCount++;
            }
            if (mol2ColCount > 1) {
                addWarningMessage("More than one Mol2 compatible column in "
                        + "input, using column \"" + mol2Column + "\".");
            }
        }
        assert mol2Column != null;
        DataColumnSpec target = in.getColumnSpec(mol2Column);
        if (target == null) {
            throw new InvalidSettingsException(
                    "Column \"" + mol2Column
                    + "\" not contained in input, please configure");
        }
        if (!target.getType().isCompatible(Mol2Value.class)) {
            throw new InvalidSettingsException(
                    "Invalid type of selected column \"" + mol2Column
                    + "\", expected Mol2 compatible type");
        }
    }

    /** Checks whether the target file is valid. Subclasses can overwrite
     * this method if they write to a different location, ignoring the file
     * location set on this object.
     * @throws InvalidSettingsException If the file is invalid.
     */
    protected void checkFileAccess() throws InvalidSettingsException {
        String outputFile = getOutputFile();
        if (outputFile == null) {
            throw new InvalidSettingsException("No output file specified");
        }
        File outFile = new File(outputFile);
        if (outFile.isDirectory()) {
            throw new InvalidSettingsException("Unable to overwrite directory: "
                    + outFile.getAbsolutePath());
        }
        File parent = outFile.getParentFile();
        if (!parent.canWrite()) {
            throw new InvalidSettingsException(
                    "Unable to write directory " + parent.getAbsolutePath());
        }
        boolean overwriteOK = isOverwriteOK();
        if (outFile.exists()) {
            if (overwriteOK) {
                addWarningMessage("Output file \""
                        + outFile.getAbsolutePath()
                        + "\" exists and will be overwritten.");
            } else {
                throw new InvalidSettingsException("File exists and can't be "
                        + "overwritten, check dialog settings");
            }
        }
    }

    /** Opens a new writer object, writing to the destination file/stream.
     * Subclasses can overwrite this method and open a stream to custom target.
     * @return A new writer object.
     * @throws IOException If the opening fails.
     * @throws InvalidSettingsException If the settings are invalid, e.g. when
     * an existing file must not be overwritten.
     */
    protected BufferedWriter openWriter()
        throws IOException, InvalidSettingsException {
        String outputFile = getOutputFile();
        File outFile = new File(outputFile);
        if (outFile.exists() && !isOverwriteOK()) {
            throw new InvalidSettingsException("File exists and can't be "
                    + "overwritten, check dialog settings");
        }
        return new BufferedWriter(new FileWriter(outFile));
    }

    /** Sets (or adds) a warning message that can be fetched by calling
     * {@link #clearWarningMessage()}.
     * @param message The message to set.
     */
    protected void addWarningMessage(final String message) {
        if (m_warningMessage == null) {
            m_warningMessage = message;
        } else {
            m_warningMessage = m_warningMessage + "\n" + message;
        }
    }

    /** Get and clear the warning message. Subsequent calls will return null
     * unless {@link #addWarningMessage(String)} is called in between.
     * @return The latest warning message (may be null).
     */
    public String clearWarningMessage() {
        String message = m_warningMessage;
        m_warningMessage = null;
        return message;
    }
}
