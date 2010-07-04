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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   21.08.2008 (thor): created
 */
package org.knime.exttool.chem.filetype.sdf;

import static org.knime.chem.base.util.sdf.SDFAnalyzer.LINE_SEP;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.knime.chem.base.util.sdf.DataItem;
import org.knime.chem.base.util.sdf.MolfileBlock;
import org.knime.chem.base.util.sdf.SDFAnalyzer;
import org.knime.chem.base.util.sdf.SDFBlock;
import org.knime.chem.types.CtabValue;
import org.knime.chem.types.MolValue;
import org.knime.chem.types.SdfValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This is the model for the SDF writer node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class SDFWriter {

    private final SDFWriterSettings m_settings;
    private String m_warningMessage;

    /** The user initials (2 characters) for use in the Molfile header. */
    public static final String USER_INITIALS;
    static {
        String user = System.getProperty("user.name").toUpperCase();
        if (user.length() >= 2) {
            USER_INITIALS = user.substring(0, 2);
        } else {
            USER_INITIALS = "??";
        }
    }

    /** Create new object, intializing settings with defaults. */
    public SDFWriter() {
        this(new SDFWriterSettings());
    }

    /** Create new writer object given a writer settings object.
     * @param settings The settings to use, must not be null.
     */
    protected SDFWriter(final SDFWriterSettings settings) {
        if (settings == null) {
            throw new NullPointerException("Argument must not be null");
        }
        m_settings = settings;
    }

    /** Set a local warning message, called from configure/execute. */
    private void setWarningMessage(final String message) {
        m_warningMessage = message;
    }

    /** Get the current warning message, whereby the message will is cleared
     * after this message returns.
     * @return The currently set warning message or null if non has been set
     * (e.g. because this method has just recently been called).
     */
    public String clearWarningMessage() {
        String result = m_warningMessage;
        m_warningMessage = null;
        return result;
    }

    /** Performs a node's configure step.
     * @param inSpec The input table spec.
     * @throws InvalidSettingsException If settings/input are invalid.
     * @see org.knime.core.node.NodeModel
     */
    protected void configure(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        if (m_settings.structureColumn() == null) {
            for (DataColumnSpec cs : inSpec) {
                if (cs.getType().isCompatible(SdfValue.class)) {
                    m_settings.structureColumn(cs.getName());
                    setWarningMessage("Selected '" + cs.getName() + "' as"
                            + " structure column");
                    break;
                }
            }
        }

        if (m_settings.structureColumn() == null) {
            for (DataColumnSpec cs : inSpec) {
                if (cs.getType().isCompatible(MolValue.class)) {
                    m_settings.structureColumn(cs.getName());
                    setWarningMessage("Selected '" + cs.getName() + "' as"
                            + " structure column");
                    break;
                }
            }
        }

        if (m_settings.structureColumn() == null) {
            for (DataColumnSpec cs : inSpec) {
                if (cs.getType().isCompatible(CtabValue.class)) {
                    m_settings.structureColumn(cs.getName());
                }
                setWarningMessage("Selected '" + cs.getName() + "' as"
                        + " structure column");
                break;
            }
        }

        if (m_settings.structureColumn() == null) {
            throw new InvalidSettingsException("No structure column selected");
        }

        if (!inSpec.containsName(m_settings.structureColumn())) {
            throw new InvalidSettingsException("Structure column '"
                    + m_settings.structureColumn()
                    + "' does not exist in input table");
        }

        if ((m_settings.titleColumn() != null)
                && !SDFWriterSettings.ROW_KEY_IDENTIFIER.equals(m_settings
                        .titleColumn())
                && !inSpec.containsName(m_settings.titleColumn())) {
            throw new InvalidSettingsException("Title column '"
                    + m_settings.titleColumn()
                    + "' does not exist in input table");
        }

        if (!m_settings.includeAllColumns()) {
            for (String p : m_settings.properties()) {
                if (!inSpec.containsName(p)) {
                    throw new InvalidSettingsException("Property column '" + p
                            + "' does not exist in input table");
                }
            }
        }

        if (m_settings.fileName() == null) {
            throw new InvalidSettingsException("No output file selected");
        }

        checkFileAccess();

    }

    /** Called by both configure and execute to check file existence. Depending
     * upon the {@link SDFWriterSettings#overwriteOK()} this will throw an
     * exception.
     * @throws InvalidSettingsException If the file to write to exists and
     * overwriting the file is not allowed.
     */
    protected void checkFileAccess() throws InvalidSettingsException {
        File f = new File(m_settings.fileName());
        if (f.exists()) {
            if (m_settings.overwriteOK()) {
                setWarningMessage("Output file '" + m_settings.fileName()
                        + "' already exists");
            } else {
                throw new InvalidSettingsException("File exists and can't be "
                        + "overwritten, check dialog settings");
            }
        }
    }

    /** Performs a node's execute method, i.e. it will write the sd file
     * according to the current settings.
     * @param inSpec The table input spec
     * @param it A iterator for the data (one scan only!)
     * @param rowCount The number of rows returned by the iterator.
     * @param exec the execution context
     * @throws Exception In case of problems.
     * @see org.knime.core.node.NodeModel
     */
    public void execute(final DataTableSpec inSpec, final RowIterator it,
            final int rowCount, final ExecutionMonitor exec) throws Exception {

        int structureIndex =
                inSpec.findColumnIndex(m_settings.structureColumn());
        DataColumnSpec structureSpec =
                inSpec.getColumnSpec(m_settings.structureColumn());
        final boolean isSDF =
                structureSpec.getType().isCompatible(SdfValue.class);
        final boolean isMol =
                structureSpec.getType().isCompatible(MolValue.class);
        final boolean isCtab =
                structureSpec.getType().isCompatible(CtabValue.class);

        int titleIndex = -1;
        if (m_settings.titleColumn() != null) {
            if (SDFWriterSettings.ROW_KEY_IDENTIFIER.equals(m_settings
                    .titleColumn())) {
                titleIndex = -2;
            } else {
                titleIndex = inSpec.findColumnIndex(m_settings.titleColumn());
            }
        }

        List<String> properties;
        if (m_settings.includeAllColumns()) {
            properties = new ArrayList<String>();

            for (DataColumnSpec dcs : inSpec) {
                DataType type = dcs.getType();
                if (type.isCompatible(IntValue.class)
                        || type.isCompatible(DoubleValue.class)
                        || (type.isASuperTypeOf(StringCell.TYPE) && StringCell.TYPE
                                .isASuperTypeOf(type))) {
                    properties.add(dcs.getName());
                }
            }
        } else {
            properties = m_settings.properties();
        }

        int[] propIndices = new int[properties.size()];
        for (int i = 0; i < propIndices.length; i++) {
            propIndices[i] = inSpec.findColumnIndex(properties.get(i));
        }

        final boolean unmodified =
                (m_settings.titleColumn() == null)
                        && !structureSpec.getType().isCompatible(
                                CtabValue.class) && (propIndices.length == 0);

        final double max = rowCount;

        BufferedWriter out = openOutputWriter();

        int count = 0;
        if (unmodified) {
            while (it.hasNext()) {
                DataRow row = it.next();
                if (count++ % 10 == 0) {
                    exec.checkCanceled();
                    exec.setProgress(count / max, "Wrote " + count
                            + " molecules");
                }
                DataCell cell = row.getCell(structureIndex);
                if (cell.isMissing()) {
                    continue;
                }
                if (isSDF) {
                    SdfValue sdf = (SdfValue)cell;
                    String s = sdf.getSdfValue();
                    out.write(s);
                    if (!s.trim().endsWith("$$$$")) {
                        if (!s.endsWith("\n")) {
                            out.write(LINE_SEP);
                        }
                        out.write("$$$$");
                        out.write(LINE_SEP);
                    }
                } else {
                    assert isMol;
                    MolValue mol = (MolValue)row.getCell(structureIndex);
                    out.write(mol.getMolValue());
                    out.write("$$$$");
                    out.write(LINE_SEP);
                }
            }
        } else {
            DateFormat format = new SimpleDateFormat("MMddyyHHmm");
            String date = format.format(new Date());

            while (it.hasNext()) {
                DataRow row = it.next();
                if (count++ % 10 == 0) {
                    exec.checkCanceled();
                    exec.setProgress(count / max, "Wrote " + count
                            + " molecules");
                }

                DataCell cell = row.getCell(structureIndex);
                if (cell.isMissing()) {
                    continue;
                }
                if (isSDF) {
                    SdfValue sdfValue = (SdfValue)cell;
                    SDFBlock sdf =
                            SDFAnalyzer.analyzeSDF(sdfValue.getSdfValue());
                    if (titleIndex == -2) {
                        sdf.getMolfileBlock().setTitle(row.getKey().toString());
                    } else if (titleIndex != -1) {
                        sdf.getMolfileBlock().setTitle(
                                row.getCell(titleIndex).toString());
                    }

                    int i = 0;
                    for (String p : properties) {
                        DataCell c = row.getCell(propIndices[i]);
                        DataItem item;
                        if (c.isMissing()) {
                            item = new DataItem(p, "");
                        } else {
                            item = new DataItem(p, c.toString());
                        }
                        sdf.addProperty(item);
                        i++;
                    }

                    out.write(sdf.toString());
                } else {
                    if (isCtab) {
                        if (titleIndex == -2) {
                            out.write(row.getKey().toString());
                        } else if (titleIndex != -1) {
                            out.write(row.getCell(titleIndex).toString());
                        }
                        out.write(LINE_SEP);

                        out.write(USER_INITIALS);
                        out.write("KNIME   ");
                        out.write(date);
                        out.write(LINE_SEP);
                        out.write(LINE_SEP);
                        out.write(((CtabValue)row.getCell(structureIndex))
                                .getCtabValue());
                    } else {
                        assert isMol;

                        MolValue molValue =
                                (MolValue)row.getCell(structureIndex);
                        MolfileBlock mol =
                                SDFAnalyzer.analyzeMolfile(molValue
                                        .getMolValue());
                        if (titleIndex == -2) {
                            mol.setTitle(row.getKey().toString());
                        } else if (titleIndex != -1) {
                            mol.setTitle(row.getCell(titleIndex).toString());
                        }
                        out.write(mol.toString());
                    }

                    int i = 0;
                    for (String p : properties) {
                        DataItem item =
                                new DataItem(p, row.getCell(propIndices[i])
                                        .toString());
                        out.write(item.toString());
                        out.write(LINE_SEP);
                        i++;
                    }
                    out.write("$$$$");
                    out.write(LINE_SEP);
                }
            }
        }

        out.close();
    }

    /** Open the output stream for writing. Sub-classes can overwrite
     * this method if they want to write to a different location
     * @return The writer instance.
     * @throws InvalidSettingsException If overwriting an existing file
     * is not allowed
     * @throws IOException If the stream opening fails
     */
    protected BufferedWriter openOutputWriter()
            throws InvalidSettingsException, IOException {
        File outFile = new File(m_settings.fileName());
        if (outFile.exists() && !m_settings.overwriteOK()) {
            throw new InvalidSettingsException("File exists and can't be "
                    + "overwritten, check dialog settings");
        }

        BufferedWriter out;
        if (m_settings.fileName().endsWith(".gz")) {
            out = new BufferedWriter(new OutputStreamWriter(
                    new GZIPOutputStream(new FileOutputStream(outFile))));
        } else {
            out = new BufferedWriter(new FileWriter(outFile));
        }
        return out;
    }

    /** Counterpart to node's loadValidatedSettingsFrom method.
     * @param settings to load from
     * @throws InvalidSettingsException If the settings aren't valid.
     */
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /** Counterpart to node's saveSettingsTo method.
     * @param settings to save to
     */
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /** Counterpart to node's validateSettings method.
     * @param settings to validate
     * @throws InvalidSettingsException If the settings aren't valid.
     */
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        SDFWriterSettings s = new SDFWriterSettings();
        s.loadSettings(settings);
    }
}
