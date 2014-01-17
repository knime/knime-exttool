/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
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
 *   Jan 18, 2010 (wiswedel): created
 */
package org.knime.exttool.node;

import java.util.Arrays;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.util.DataValueColumnFilter;
import org.knime.exttool.executor.Execution;

/** A configuration object to customize properties of external tool
 * node extensions. In particular it allows setting
 * <ul>
 * <li>the number of in- and output ports (defaults to 1:1)
 * <li>the visibility of different controls in the dialog
 *     (e.g. to disable the input/output tabs)
 * <li>a pre-filtering of the appropriate input columns (for instance if
 *     a derived node only handles certain input types)
 * </ul>
 * This class is also meant to be subclassed in order to return, e.g. a
 * specialized dialog panel with custom controls (buttons, checkboxes, etc.).
 * Subclasses will overwrite methods such as
 * {@link #createCommandlineSettings()}.
 *
 * This class is used in (extensions of) the {@link ExttoolNodeFactory} to do
 * a custom setup of the node. Although almost all properties in this class
 * have both a set and get method, the set methods must only be called right
 * after instantiation of this object (which is typically in the
 * constructor of the node factory).
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class ExttoolCustomizer {

    /** Whether (and which) temp files are deleted after execution. */
    public enum DeleteTempFilePolicy {
        /** Delete all files, independent of success of failure. */
        All,
        /** Delete only chunks with successful execution. */
        OnlyNonFailed,
        /** Delete no files. */
        None;

        /** Parses the argument, throws exception if invalid.
         * @param value The value to parse
         * @return The corresponding enum value.
         * @throws InvalidSettingsException If that fails.
         */
        public static DeleteTempFilePolicy read(final String value)
            throws InvalidSettingsException {
            if (value == null) {
                throw new InvalidSettingsException("Value for "
                        + DeleteTempFilePolicy.class.getSimpleName()
                        + " is null");
            }
            try {
                return valueOf(value);
            } catch (IllegalArgumentException iae) {
                throw new InvalidSettingsException("Invalid value for "
                        + DeleteTempFilePolicy.class.getSimpleName()
                        + ": " + value);
            }
        }
    }

    /** Partitioning the input table. */
    public enum Chunking {
        /** All rows in one chunk. */
        EntireTable,
        /** Each row is a chunk. */
        IndividualRow,
        /** Chunks with a certain number of rows. */
        ChunksOfSize,
        /** Given number of chunks, number of rows per chunk set accordingly. */
        NrChunks;

        /** Parses the argument, throws exception if invalid.
         * @param value The value to parse
         * @return The corresponding enum value.
         * @throws InvalidSettingsException If that fails.
         */
        public static Chunking read(final String value)
            throws InvalidSettingsException {
            if (value == null) {
                throw new InvalidSettingsException("Value for "
                        + Chunking.class.getSimpleName()
                        + " is null");
            }
            try {
                return valueOf(value);
            } catch (IllegalArgumentException iae) {
                throw new InvalidSettingsException("Invalid value for "
                        + Chunking.class.getSimpleName()
                        + ": " + value);
            }
        }
    }

    private final int m_nrInputs;
    private final int m_nrOutputs;

    private boolean m_showChunkSizeHandlingPanel = true;
    private boolean m_showPathToExecutableField = false;
    private boolean m_showPathToTempInputFile = true;
    private boolean m_showPathToTempOutputFile = true;
    private boolean m_showOutputTypeSelection = true;
    private boolean m_showTabInputFile = true;
    private boolean m_showTabOutputFile = true;
    private boolean m_showTabExecutorPanel = true;
    private DeleteTempFilePolicy m_defaultDeleteTempFilePolicy =
        DeleteTempFilePolicy.All;

    private String m_executableFileHistoryID = "exttool";
    private String[] m_executableFileHistorySuffixes = new String[0];

    @SuppressWarnings("unchecked")
    private ColumnFilter m_columnFilter =
        new DataValueColumnFilter(DataValue.class);

    /** Create new customizer for nodes with one in- and one output. */
    public ExttoolCustomizer() {
        this(1, 1);
    }

    /** Create a customizer for nodes with a given number of
     * in- and outports.
     * @param ins number of inputs.
     * @param outs number of outputs. */
    public ExttoolCustomizer(final int ins, final int outs) {
        if (ins < 0) {
            throw new IllegalArgumentException("Illegal port count: " + ins);
        }
        if (outs < 0) {
            throw new IllegalArgumentException("Illegal port count: " + outs);
        }
        m_nrInputs = ins;
        m_nrOutputs = outs;
    }

    /** Create a custom commandline settings objects. Subclasses overwrite this
     * method to return their custom dialog settings (and panel). This method
     * is called often, each invocation must return a new object of same class.
     * @return A <b>new</b> instance of the  command line settings, this
     * implementation returns a new instance of
     * {@link FreeFormCommandlineSettings}.
     */
    protected AbstractCommandlineSettings createCommandlineSettings() {
        return new FreeFormCommandlineSettings();
    }

    /** Create a new settings object that is used to persist the node
     * configuration. Subclasses may override this to return an extension
     * of {@link ExttoolSettings}. This method is called frequently (several
     * times when a dialog is opened or closed).
     * @return A new settings object.
     */
    protected ExttoolSettings createExttoolSettings() {
        return new ExttoolSettings(this);
    }

    /** Create a new {@link Execution} that performs the run. This method
     * is called during the node's execution.
     * @param settings The current settings to the node.
     * @param env The current execution environment
     * @return A new execution object.
     */
    protected Execution createExecution(final ExttoolSettings settings,
            final ExttoolNodeEnvironment env) {
        return new Execution(this, settings, env);
    }

    /** Modifies the input tables that are passed to the external tool
     * framework (dialog and execution). Sub classes can overwrite the
     * {@link #createPreprocessColumnRearranger(DataTableSpec[])} to control
     * this preprocessing step and, for instance append columns that
     * are required by the external process.
     *
     * @param input The input table specs
     * @return The preprocessed input.
     */
    public final DataTableSpec[] preprocessInput(final DataTableSpec[] input) {
        ColumnRearranger[] cr = createPreprocessColumnRearranger(input);
        if (cr == null) {
            return input;
        }
        DataTableSpec[] newIns = new DataTableSpec[input.length];
        for (int i = 0; i < input.length; i++) {
            ColumnRearranger r = cr[i];
            newIns[i] = r.createSpec();
        }
        return newIns;
    }

    /** Modifies the input tables prior execution. It does so according
     * to the (overwritten)
     * {@link #createPreprocessColumnRearranger(DataTableSpec[])} method.
     *
     * @param input The original input data.
     * @param exec The execution context in case columns are removed or new
     *             data is generated.
     * @return The freshly prepared input data that is passed to the
     *         external tool framework (file writers and so on).
     * @throws CanceledExecutionException If canceled
     */
    public final BufferedDataTable[] preprocessInput(
            final BufferedDataTable[] input, final ExecutionContext exec)
    throws CanceledExecutionException {
        DataTableSpec[] inSpecs = new DataTableSpec[input.length];
        for (int i = 0; i < input.length; i++) {
            inSpecs[i] = input[i].getDataTableSpec();
        }
        ColumnRearranger[] cr = createPreprocessColumnRearranger(inSpecs);
        if (cr == null) {
            return input;
        }
        BufferedDataTable[] newIns = new BufferedDataTable[input.length];
        for (int i = 0; i < input.length; i++) {
            ExecutionContext sub = exec;
            if (input.length > 1) {
                sub = exec.createSubExecutionContext(1.0 / input.length);
                exec.setMessage("Port " + i);
            }
            ColumnRearranger r = cr[i];
            newIns[i] = sub.createColumnRearrangeTable(input[i], r, sub);
        }
        return newIns;
    }

    /** Allows sub-classes to modify the input data that is passed to the
     * external tool framework. This preprocessing is static for a given tool
     * (nodes always append the same column, independent of the input data)
     * and therefore independent of the actual {@link ExttoolSettings}.
     *
     * <p>The default implementation returns <code>null</code>, i.e. no
     * modification is done.
     *
     * @param ins The original input specs.
     * @return The rearranger to modify the input data. Returning
     * <code>null</code> is OK (no change to input) but non-null return values
     * must not contain null.
     */
    protected ColumnRearranger[] createPreprocessColumnRearranger(
            final DataTableSpec[] ins) {
        return null;
    }

    /** @return the nrInputs (constructor argument) */
    public int getNrInputs() {
        return m_nrInputs;
    }

    /** @return the nrOutputs (constructor argument) */
    public int getNrOutputs() {
        return m_nrOutputs;
    }

    /** Whether to show the panel with the chunk size handling.
     * @return the showChunkSizeHandlingPanel property
     */
    public boolean isShowChunkSizeHandlingPanel() {
        return m_showChunkSizeHandlingPanel;
    }

    /** Set whether to show the chunk size panel.
     * @param showChunkSizeHandlingPanel the showChunkSizeHandlingPanel to set
     */
    public void setShowChunkSizeHandlingPanel(
            final boolean showChunkSizeHandlingPanel) {
        m_showChunkSizeHandlingPanel = showChunkSizeHandlingPanel;
    }

    /** Whether to show the path to the executable (text field with history).
     * @return the showPathToExecutableField property
     */
    public boolean isShowPathToExecutableField() {
        return m_showPathToExecutableField;
    }

    /** Set whether to show the path to the executable
     *  (text field with history).
     * @param showPathToExecutableField the showPathToExecutableField property
     */
    public void setShowPathToExecutableField(
            final boolean showPathToExecutableField) {
        m_showPathToExecutableField = showPathToExecutableField;
    }

    /**
     * @return the showPathTempInputFile
     */
    public boolean isShowPathToTempInputFile() {
        return m_showPathToTempInputFile;
    }

    /**
     * @param showPathToTempInputFile the showPathTempInputFile to set
     */
    public void setShowPathToTempInputFile(
            final boolean showPathToTempInputFile) {
        m_showPathToTempInputFile = showPathToTempInputFile;
    }

    /**
     * @return the showPathTempOutputFile
     */
    public boolean isShowPathToTempOutputFile() {
        return m_showPathToTempOutputFile;
    }

    /**
     * @param showPathToTempOutputFile the showPathTempOutputFile to set
     */
    public void setShowPathToTempOutputFile(
            final boolean showPathToTempOutputFile) {
        m_showPathToTempOutputFile = showPathToTempOutputFile;
    }

    /**
     * @param showOutputTypeSelection the showOutputTypeSelection to set
     */
    public void setShowOutputTypeSelection(
            final boolean showOutputTypeSelection) {
        m_showOutputTypeSelection = showOutputTypeSelection;
    }

    /**
     * @return the showOutputTypeSelection
     */
    public boolean isShowOutputTypeSelection() {
        return m_showOutputTypeSelection;
    }

    /**
     * @return the showTabInputFile
     */
    public boolean isShowTabInputFile() {
        return m_showTabInputFile;
    }

    /**
     * @param showTabInputFile the showTabInputFile to set
     */
    public void setShowTabInputFile(final boolean showTabInputFile) {
        m_showTabInputFile = showTabInputFile;
    }

    /**
     * @return the showTabOutputFile
     */
    public boolean isShowTabOutputFile() {
        return m_showTabOutputFile;
    }

    /**
     * @param showTabOutputFile the showTabOutputFile to set
     */
    public void setShowTabOutputFile(final boolean showTabOutputFile) {
        m_showTabOutputFile = showTabOutputFile;
    }

    /**
     * @return the showTabExecutorPanel
     */
    public boolean isShowTabExecutorPanel() {
        return m_showTabExecutorPanel;
    }

    /**
     * @param showTabExecutorPanel the showTabExecutorPanel to set
     */
    public void setShowTabExecutorPanel(final boolean showTabExecutorPanel) {
        m_showTabExecutorPanel = showTabExecutorPanel;
    }

    /**
     * @return the executableFileHistoryID
     */
    public String getExecutableFileHistoryID() {
        return m_executableFileHistoryID;
    }

    /**
     * @param executableFileHistoryID the executableFileHistoryID to set
     */
    public void setExecutableFileHistoryID(
            final String executableFileHistoryID) {
        if (executableFileHistoryID == null) {
            throw new NullPointerException("Argument must not be null");
        }
        if (m_executableFileHistoryID.trim().length() == 0) {
            throw new IllegalArgumentException("Illegal history id: \""
                    + m_executableFileHistoryID + "\"");
        }
        m_executableFileHistoryID = executableFileHistoryID;
    }

    /**
     * @return the executableFileHistorySuffixes
     */
    public String[] getExecutableFileHistorySuffixes() {
        return m_executableFileHistorySuffixes;
    }

    /**
     * @param suffixes the executableFileHistorySuffixes to set
     */
    public void setExecutableFileHistorySuffixes(
            final String... suffixes) {
        if (suffixes == null || Arrays.asList(suffixes).contains(null)) {
            throw new NullPointerException("Argument must not be null or "
                    + "contain null elements");
        }
        m_executableFileHistorySuffixes = suffixes;
    }

    /**
     * @param defaultDeleteTempFilePolicy the defaultDeleteTempFilePolicy to set
     * @throws NullPointerException If argument is null.
     */
    public void setDefaultDeleteTempFilePolicy(
            final DeleteTempFilePolicy defaultDeleteTempFilePolicy) {
        if (defaultDeleteTempFilePolicy == null) {
            throw new NullPointerException("Argument must not be null");
        }
        m_defaultDeleteTempFilePolicy = defaultDeleteTempFilePolicy;
    }

    /**
     * @return the defaultDeleteTempFilePolicy
     */
    public DeleteTempFilePolicy getDefaultDeleteTempFilePolicy() {
        return m_defaultDeleteTempFilePolicy;
    }

    /** @param columnFilter the columnFilter to set */
    public void setColumnFilter(final ColumnFilter columnFilter) {
        if (columnFilter == null) {
            throw new NullPointerException("Argument must not be null");
        }
        m_columnFilter = columnFilter;
    }

    /** Reduce the number of selectable input columns to the argument type list.
     * @param valClass A class of DataValue class that are accepted
     * @see #setColumnFilter(ColumnFilter)
     * @see DataValueColumnFilter
     * @throws NullPointerException If the argument is <code>null</code> or
     * contains <code>null</code> arguments.
     */
    public void setColumnFilter(final Class<? extends DataValue>... valClass) {
        setColumnFilter(new DataValueColumnFilter(valClass));
    }

    /** @return the columnFilter */
    public ColumnFilter getColumnFilter() {
        return m_columnFilter;
    }

}
