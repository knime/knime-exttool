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
 *   Mar 9, 2010 (wiswedel): created
 */
package org.knime.exttool.chem.filetype.mol2;

import static org.knime.chem.base.util.sdf.SDFAnalyzer.LINE_SEP;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.knime.chem.types.Mol2AdapterCell;
import org.knime.chem.types.Mol2CellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;

/** Reader implementation for Mol2 files. The typical use case is: create a new
 * instance, set parameters (e.g. {@link #setExtractMolName(boolean)}) and
 * finally call the {@link #createOutputSpec()} or
 * {@link #execute(ExecutionContext)} method. This class can be overwritten
 * to allow for more flexible source locations.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class Mol2Reader {

    /** Record start identifier. */
    static final String TRIPOS_MOLECULE = "@<TRIPOS>MOLECULE";

    /** Column spec for the structure column. */
    public static final DataColumnSpec MOLECULE_COL_SPEC =
            new DataColumnSpecCreator("Molecule", Mol2AdapterCell.RAW_TYPE).createSpec();

    /** Column spec for name column (if {@link #isExtractMolName()}). */
    public static final DataColumnSpec MOLECULE_COLNAME_SPEC =
        new DataColumnSpecCreator("Molecule Name",
                StringCell.TYPE).createSpec();

    private String m_fileName;
    private boolean m_generateID;
    private boolean m_extractMolName;

    /**
     * @return the fileName
     */
    public String getFileName() {
        return m_fileName;
    }
    /**
     * @param fileName the fileName to set
     */
    public void setFileName(final String fileName) {
        m_fileName = fileName;
    }
    /**
     * @return the generateID
     */
    public boolean isGenerateID() {
        return m_generateID;
    }
    /**
     * @param generateID the generateID to set
     */
    public void setGenerateID(final boolean generateID) {
        m_generateID = generateID;
    }
    /**
     * @return the extractMolName
     */
    public boolean isExtractMolName() {
        return m_extractMolName;
    }
    /**
     * @param extractMolName the extractMolName to set
     */
    public void setExtractMolName(final boolean extractMolName) {
        m_extractMolName = extractMolName;
    }

    /** Called from a node's configure method to determine the output spec. This
     * method also validates the input file location.
     * @return The output spec.
     * @throws InvalidSettingsException If the configuration isn't valid.
     */
    public DataTableSpec createOutputSpec() throws InvalidSettingsException {
        checkSourceAccess();
        List<DataColumnSpec> colSpecList = new ArrayList<DataColumnSpec>(2);
        colSpecList.add(MOLECULE_COL_SPEC);
        if (isExtractMolName()) {
            colSpecList.add(MOLECULE_COLNAME_SPEC);
        }
        return new DataTableSpec(colSpecList.toArray(
                new DataColumnSpec[colSpecList.size()]));
    }

    /** Checks whether the source (file) can be accessed. Subclasses can
     * overwrite this method and, for instance, ignore the validation.
     * @throws InvalidSettingsException If the input isn't valid.
     */
    protected void checkSourceAccess() throws InvalidSettingsException {
        if (m_fileName == null) {
            throw new InvalidSettingsException("No Mol2 file specified");
        }
        File f = new File(m_fileName);
        if (!f.exists()) {
            throw new InvalidSettingsException("Mol2 file '"
                    + f.getAbsolutePath() + "' does not exist.");
        }
        if (!f.isFile()) {
            throw new InvalidSettingsException("The path '"
                    + f.getAbsolutePath() + "' is not a file.");
        }
    }

    /** Opens a reader on the source. Subclasses can overwrite this method
     * and return a custom reader.
     * @return A new reader instance on the source.
     * @throws IOException If that fails due to I/O problems.
     */
    protected BufferedReader openReader() throws IOException {
        File f = new File(m_fileName);
        BufferedReader in;
        if (f.getName().endsWith(".gz")) {
            in = new BufferedReader(new InputStreamReader(
                    new GZIPInputStream(new FileInputStream(f))));
        } else {
            in = new BufferedReader(new FileReader(f));
        }
        return in;
    }

    /** Performs the read logic, called from the node's execute method.
     * @param exec to report progress to.
     * @return the table being read.
     * @throws Exception If that fails for any reason.
     */
    public BufferedDataTable execute(final ExecutionContext exec)
        throws Exception {
        BufferedDataContainer cont =
            exec.createDataContainer(createOutputSpec());
        BufferedReader in = openReader();
        HashSet<String> titles = new HashSet<String>();

        String line;
        StringBuilder buf = new StringBuilder(4096);
        String title = "";
        String molName = "";
        boolean stringBufferContainsMolecule = false;
        int count = 1;
        exec.setMessage("Reading molecule #" + count);
        while ((line = in.readLine()) != null) {
            exec.checkCanceled();

            if (line.startsWith(TRIPOS_MOLECULE)) {
                if (stringBufferContainsMolecule) {
                    addMol(buf, title, molName, cont);
                    count++;
                }
                stringBufferContainsMolecule = true;
                buf.append(line).append(LINE_SEP);
                line = in.readLine();
                // encapsulate the row key string in a new string to truncate
                // the data (see bug #1737)
                molName = line == null ? null : new String(line);
                buf.append(line).append(LINE_SEP);
                if (m_generateID || (molName == null)
                        || (molName.length() == 0)) {
                    title = "Mol " + count;
                } else {
                    title = molName;
                }
                if (titles.contains(title)) {
                    title = title + "-" + count;
                }
                titles.add(title);
                exec.setMessage("Reading molecule #" + count);
            } else {
                buf.append(line).append(LINE_SEP);
            }
        }

        if (stringBufferContainsMolecule) {
            addMol(buf, title, molName, cont);
        }

        cont.close();
        in.close();

        return cont.getTable();
    }

    /** Adds a row to the output container.
     * @param buf the content of the record
     * @param title the associated title
     * @param moleculeName name of record (line following &lt;TRIPOS>Molecule)
     * @param cont container to add to.
     */
    private void addMol(final StringBuilder buf, final String title,
            final String moleculeName, final DataContainer cont) {
        DataCell mol2Cell = Mol2CellFactory.createAdapterCell(buf.toString());
        RowKey key = new RowKey(title);
        DataCell[] cells;
        if (m_extractMolName) {
            String name = moleculeName == null ? null : moleculeName;
            StringCell molNameCell = new StringCell(name);
            cells = new DataCell[] {mol2Cell, molNameCell};
        } else {
            cells = new DataCell[] {mol2Cell};
        }
        DefaultRow row = new DefaultRow(key, cells);
        cont.addRowToTable(row);
        buf.delete(0, buf.length());
    }

}
