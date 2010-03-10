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
 *   19.08.2008 (thor): created
 */
package org.knime.exttool.chem.filetype.sdf;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.knime.base.node.util.BufferedFileReader;
import org.knime.chem.base.util.sdf.DataItem;
import org.knime.chem.base.util.sdf.SDFAnalyzer;
import org.knime.chem.base.util.sdf.SDFBlock;
import org.knime.chem.types.CtabCell;
import org.knime.chem.types.CtabCellFactory;
import org.knime.chem.types.MolCell;
import org.knime.chem.types.MolCellFactory;
import org.knime.chem.types.SdfCell;
import org.knime.chem.types.SdfCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.exttool.chem.filetype.sdf.SDFReaderSettings.Property;

/**
 * This is the model for the SDF reader node. It takes an SDF file (actually any
 * readable URL) and splits it into the different SDF molecules. Optionally,
 * different parts from the SDF blocks, like the Molfile block, the Ctab and the
 * properties are also extracted.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class SDFReader {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(SDFReader.class);

    private static final DataTableSpec BROKEN_RECORDS_SPEC;
    static {
        DataColumnSpec dcs1 =
                new DataColumnSpecCreator("SDF string", StringCell.TYPE)
                        .createSpec();
        DataColumnSpec dcs2 =
                new DataColumnSpecCreator("Error", StringCell.TYPE)
                        .createSpec();
        BROKEN_RECORDS_SPEC = new DataTableSpec(dcs1, dcs2);
    }

    private final SDFReaderSettings m_settings;

    private String m_warningMessage;

    /** Creates new reader, providing settings object as argument.
     * @param settings The settings to use.
     */
    protected SDFReader(final SDFReaderSettings settings) {
        if (settings == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_settings = settings;
    }

    /** Creates new reader with standard settings. */
    public SDFReader() {
        this(new SDFReaderSettings());
    }

    /**
     * @param warningMessage the warningMessage to set
     */
    protected void setWarningMessage(final String warningMessage) {
        m_warningMessage = warningMessage;
    }

    /**
     * @return the warningMessage
     */
    public String clearWarningMessage() {
        String message = m_warningMessage;
        m_warningMessage = null;
        return message;
    }

    /** Performs the node's configure step. Returns a spec representing
     * the output table specs.
     * @return An array of length 2, the 1st element represents the data, which
     * could be read successfully, the 2nd element the failed ones.
     * @throws InvalidSettingsException If the configuration is invalid.
     */
    public DataTableSpec[] configure() throws InvalidSettingsException {
        if (m_settings.fileName() == null) {
            throw new InvalidSettingsException("No file selected");
        }
        try {
            openInputReader().close();
        } catch (MalformedURLException ex) {
            throw new InvalidSettingsException(ex);
        } catch (IOException ex) {
            throw new InvalidSettingsException("Cannot access '"
                    + m_settings.fileName() + "': " + ex.getMessage(), ex);
        }

        return new DataTableSpec[]{createSpec(), BROKEN_RECORDS_SPEC};
    }

    /**
     * Creates the output spec based on the settings from the dialog.
     *
     * @return the output spec
     */
    private DataTableSpec createSpec() {
        List<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>();

        if (m_settings.extractSDF()) {
            colSpecs.add(new DataColumnSpecCreator("Molecule", SdfCell.TYPE)
                    .createSpec());
        }

        if (m_settings.extractMol()) {
            colSpecs.add(new DataColumnSpecCreator("Mol Block", MolCell.TYPE)
                    .createSpec());
        }

        if (m_settings.extractCtab()) {
            colSpecs.add(new DataColumnSpecCreator("Ctab Block", CtabCell.TYPE)
                    .createSpec());
        }

        if (m_settings.extractName()) {
            colSpecs.add(new DataColumnSpecCreator("Molecule name",
                    StringCell.TYPE).createSpec());
        }

        if (m_settings.extractCounts()) {
            colSpecs.add(new DataColumnSpecCreator("Atom count", IntCell.TYPE)
                    .createSpec());
            colSpecs.add(new DataColumnSpecCreator("Bond count", IntCell.TYPE)
                    .createSpec());
        }

        for (Property p : m_settings.properties()) {
            if (p.extract) {
                DataType type;
                if (p.type == Integer.class) {
                    type = IntCell.TYPE;
                } else if (p.type == Double.class) {
                    type = DoubleCell.TYPE;
                } else {
                    type = StringCell.TYPE;
                }

                colSpecs.add(new DataColumnSpecCreator(p.name, type)
                        .createSpec());
            }
        }

        return new DataTableSpec(colSpecs.toArray(new DataColumnSpec[1]));
    }

    /** Performs a node's execute method. Reads the data and returns
     * the data as described in the {@link #configure()} method.
     * @param exec context for progress report, cancellation and table creation.
     * @return The data being read.
     * @throws Exception If that fails for any reason.
     */
    public BufferedDataTable[] execute(
            final ExecutionContext exec) throws Exception {
        DataTableSpec outSpec = createSpec();
        BufferedDataContainer cont1 = createOutputContainerPort0(exec, outSpec);
        BufferedDataContainer cont2 =
                exec.createDataContainer(BROKEN_RECORDS_SPEC);

        BufferedReader in = openInputReader();
        final double max;
        if (in instanceof BufferedFileReader) {
            max = ((BufferedFileReader)in).getFileSize();
        } else {
            max = 0;
        }

        SDFAnalyzer analyzer = new SDFAnalyzer(in);

        String mol;
        int count = 0;
        while ((mol = analyzer.nextMolecule()) != null) {
            if (count++ % 10 == 0) {
                exec.checkCanceled();
                if (max > 0 && in instanceof BufferedFileReader) {
                    double r = ((BufferedFileReader)in).getNumberOfBytesRead();
                    exec.setProgress(r / max, "Read " + count + " molecules");
                } else {
                    exec.setMessage("Read " + count + " molecules");
                }
            }

            SDFBlock sdf;
            try {
                sdf = SDFAnalyzer.analyzeSDF(mol);
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
                DataCell[] cells = new DataCell[2];
                cells[0] = new StringCell(mol);
                cells[1] = new StringCell(ex.getMessage());
                cont2.addRowToTable(new DefaultRow(RowKey.createRowKey(count),
                        cells));
                continue;
            }

            DataCell[] cells = new DataCell[outSpec.getNumColumns()];

            int k = 0;
            if (m_settings.extractSDF()) {
                cells[k++] = SdfCellFactory.create(mol);
            }

            if (m_settings.extractMol()) {
                cells[k++] =
                        MolCellFactory.create(sdf.getMolfileBlock().toString());
            }
            if (m_settings.extractCtab()) {
                cells[k++] =
                        CtabCellFactory.create(sdf.getMolfileBlock()
                                .getCtabBlock().toString());
            }

            if (m_settings.extractName()) {
                cells[k++] = new StringCell(sdf.getMolfileBlock().getTitle());
            }

            if (m_settings.extractCounts()) {
                cells[k++] =
                        new IntCell(sdf.getMolfileBlock().getCtabBlock()
                                .getAtomCount());
                cells[k++] =
                        new IntCell(sdf.getMolfileBlock().getCtabBlock()
                                .getBondCount());
            }

            Map<String, DataItem> props = sdf.getProperties();

            for (Property p : m_settings.properties()) {
                if (p.extract) {
                    DataItem item = props.get(p.name);
                    if (item == null) {
                        cells[k++] = DataType.getMissingCell();
                    } else {
                        Object v = props.get(p.name).getValue();
                        if (v == null) {
                            cells[k++] = DataType.getMissingCell();
                        } else if (p.type == Integer.class) {
                            cells[k++] = new IntCell(((Number)v).intValue());
                        } else if (p.type == Double.class) {
                            cells[k++] =
                                    new DoubleCell(((Number)v).doubleValue());
                        } else {
                            cells[k++] =
                                    new StringCell(props.get(p.name)
                                            .getUnparsedValue());
                        }
                    }
                }
            }

            // encapsulate the row key string in a new string to truncate
            // the data (see bug #1737)
            RowKey key =
                    m_settings.useRowID() ? new RowKey(new String(sdf
                            .getMolfileBlock().getTitle())) : RowKey
                            .createRowKey(count);

            cont1.addRowToTable(new DefaultRow(key, cells));
        }

        cont1.close();
        cont2.close();
        BufferedDataTable brokenTbl = cont2.getTable();
        if (brokenTbl.getRowCount() > 0) {
            setWarningMessage("Failed to parse " + brokenTbl.getRowCount()
                    + " record(s)");
        }
        return new BufferedDataTable[]{cont1.getTable(), cont2.getTable()};
    }

    /** Represents a node' loadInternals method. It checks whether the URL
     * used in the execute method is still accessible and, if not, sets an
     * appropriate warning message.
     */
    public void loadInternals() {
        /*
         * This is a special "deal" for the file reader: The file reader, if
         * previously executed, has data at it's output - even if the file that
         * was read doesn't exist anymore. In order to warn the user that the
         * data cannot be recreated we check here if the file exists and set a
         * warning message if it doesn't.
         */
        final String fileName = m_settings.fileName();
        if (fileName == null) {
            return;
        }

        try {
            URL location = getUrl(fileName);
            if (!location.toString().startsWith("file://")) {
                // We can only check files. Other protocols are ignored.
                return;
            }

            InputStream inStream = location.openStream();
            if (inStream == null) {
                setWarningMessage("The file '" + location.toString()
                        + "' can't be accessed anymore!");
            } else {
                inStream.close();
            }
        } catch (MalformedURLException ex) {
            setWarningMessage("Invalid or non-existing URL: " + fileName);
        } catch (IOException ioe) {
            setWarningMessage("The file '" + fileName
                    + "' can't be accessed anymore!");
        } catch (NullPointerException npe) {
            // thats a bug in the windows open stream
            // a path like c:\blah\ \ (space as dir) causes a NPE.
            setWarningMessage("The file '" + fileName
                    + "' can't be accessed anymore!");
        }
    }

    /** Opens an input reader on the input source. Source location is retrieved
     * from the current settings. This method can be overwritten by subclasses
     * to return a custom input source, ignoring the current settings.
     * @return An open input reader.
     * @throws IOException If that fails due to I/O problems.
     * @throws InvalidSettingsException If the settings are invalid, e.g.
     *         because of an illegal URL.
     */
    protected BufferedReader openInputReader()
        throws IOException, InvalidSettingsException {
        String fileName = m_settings.fileName();
        if (fileName == null) {
            throw new InvalidSettingsException("No file selected");
        }
        URL url;
        try {
            url = getUrl(fileName);
        } catch (MalformedURLException e) {
            throw new InvalidSettingsException("Invalid URL: " + fileName, e);
        }
        return BufferedFileReader.createNewReader(url);
    }

    /** Loads settings, which passed the
     * {@link #validateSettings(NodeSettingsRO)} test.
     * @param settings To load from.
     * @throws InvalidSettingsException If settings are incomplete.
     */
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /** Saves settings to argument.
     * @param settings to save to.
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /** Validates completeness of argument settings.
     * @param settings To validate
     * @throws InvalidSettingsException If validation fails.
     */
    public void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        SDFReaderSettings s = new SDFReaderSettings();
        s.loadSettings(settings);
    }

    /** Create the output container hosting the output data. Subclasses can
     * overwrite this method if they want to have a custom memory policy (this
     * implementation uses the user-set memory policy).
     * @param exec The create a container from.
     * @param spec The output spec for the output data.
     * @return A new data container that is filled by the
     *         {@link #execute(ExecutionContext)} method.
     */
    protected BufferedDataContainer createOutputContainerPort0(
            final ExecutionContext exec, final DataTableSpec spec) {
        return exec.createDataContainer(spec);
    }

    /**
     * Creates an URL from the "file"name entered in the dialog.
     *
     * @param fileName the file's name, may already be an URL
     * @return an URL
     * @throws MalformedURLException if the URL is malformed
     */
    protected static URL getUrl(
            final String fileName) throws MalformedURLException {
        File f = new File(fileName);
        if (f.exists()) {
            return f.toURI().toURL();
        } else {
            return new URL(fileName);
        }
    }
}
